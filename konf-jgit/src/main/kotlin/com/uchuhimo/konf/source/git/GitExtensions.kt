package com.uchuhimo.konf.source.git

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.DefaultLoaders
import com.uchuhimo.konf.source.InvalidRemoteRepoException
import com.uchuhimo.konf.source.Loader
import com.uchuhimo.konf.source.Provider
import com.uchuhimo.konf.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.transport.URIish
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Returns a child config containing values from a specified git repository.
 *
 * Format of the url is auto-detected from the url extension.
 * Supported url formats and the corresponding extensions:
 * - HOCON: conf
 * - JSON: json
 * - Properties: properties
 * - YAML: yml, yaml
 *
 * Throws [UnsupportedExtensionException] if the url extension is unsupported.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param action additional action when cloning/pulling
 * @return a child config containing values from a specified git repository
 * @throws UnsupportedExtensionException
 */
fun DefaultLoaders.git(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        action: TransportCommand<*, *>.() -> Unit = {}
): Config = dispatchExtension(File(file).extension, "{repo: $repo, file: $file}")
        .git(repo, file, dir, branch, action)

/**
 * Returns a child config containing values from a specified git repository,
 * and reloads values periodically.
 *
 * Format of the url is auto-detected from the url extension.
 * Supported url formats and the corresponding extensions:
 * - HOCON: conf
 * - JSON: json
 * - Properties: properties
 * - YAML: yml, yaml
 *
 * Throws [UnsupportedExtensionException] if the url extension is unsupported.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param period reload period. The default value is 1.
 * @param unit time unit of reload period. The default value is [TimeUnit.MINUTES].
 * @param context context of the coroutine. The default value is [DefaultDispatcher].
 * @param action additional action when cloning/pulling
 * @return a child config containing values from a specified git repository
 * @throws UnsupportedExtensionException
 */
fun DefaultLoaders.watchGit(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        period: Long = 1,
        unit: TimeUnit = TimeUnit.MINUTES,
        context: CoroutineContext = Dispatchers.Default,
        action: TransportCommand<*, *>.() -> Unit = {}
): Config = dispatchExtension(File(file).extension, "{repo: $repo, file: $file}")
        .watchGit(repo, file, dir, branch, period, unit, context, action)

/**
 * Returns a child config containing values from a specified git repository.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param action additional action when cloning/pulling
 * @return a child config containing values from a specified git repository
 */
fun Loader.git(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        action: TransportCommand<*, *>.() -> Unit = {}
): Config =
        config.withSource(provider.fromGit(repo, file, dir, branch, action))

/**
 * Returns a child config containing values from a specified git repository,
 * and reloads values periodically.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param period reload period. The default value is 1.
 * @param unit time unit of reload period. The default value is [TimeUnit.MINUTES].
 * @param context context of the coroutine. The default value is [DefaultDispatcher].
 * @param action additional action when cloning/pulling
 * @return a child config containing values from a specified git repository
 */
fun Loader.watchGit(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        period: Long = 1,
        unit: TimeUnit = TimeUnit.MINUTES,
        context: CoroutineContext = Dispatchers.Default,
        action: TransportCommand<*, *>.() -> Unit = {}
): Config {
    return (dir ?: createTempDir(prefix = "local_git_repo").path).let { directory ->
        provider.fromGit(repo, file, directory, branch, action).let { source ->
            config.withLoadTrigger("watch ${source.description}") { newConfig, load ->
                load(source)
                GlobalScope.launch(context) {
                    while (true) {
                        delay(unit.toMillis(period))
                        newConfig.lock {
                            newConfig.clear()
                            load(provider.fromGit(repo, file, directory, branch, action))
                        }
                    }
                }
            }.withLayer()
        }
    }
}


/**
 * Returns a new source from a specified git repository.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param action additional action when cloning/pulling
 * @return a new source from a specified git repository
 */
fun Provider.fromGit(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        action: TransportCommand<*, *>.() -> Unit = {}
): Source {
    return (dir?.let(::File) ?: createTempDir(prefix = "local_git_repo")).let { directory ->
        if (directory.list { _, name -> name == ".git" }.isEmpty()) {
            Git.cloneRepository().apply {
                setURI(repo)
                setDirectory(directory)
                setBranch(branch)
                this.action()
            }.call().close()
        } else {
            Git.open(directory).use { git ->
                val uri = URIish(repo)
                val remoteName = git.remoteList().call().firstOrNull { it.urIs.contains(uri) }?.name
                        ?: throw InvalidRemoteRepoException(repo, directory.path)
                git.pull().apply {
                    remote = remoteName
                    remoteBranchName = branch
                    this.action()
                }.call()
            }
        }
        fromFile(Paths.get(directory.path, file).toFile()).apply {
            addContext("repo", repo)
            addContext("file", file)
            addContext("dir", directory.path)
            addContext("branch", branch)
        }
    }
}