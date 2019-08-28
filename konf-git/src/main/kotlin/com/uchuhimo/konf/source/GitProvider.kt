/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source

import com.uchuhimo.konf.source.base.EmptyMapSource
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.transport.URIish
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * Returns a new source from a specified git repository.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param optional whether this source is optional
 * @param action additional action when cloning/pulling
 * @return a new source from a specified git repository
 */
fun Provider.fromGit(
    repo: String,
    file: String,
    dir: String? = null,
    branch: String = Constants.HEAD,
    optional: Boolean = false,
    action: TransportCommand<*, *>.() -> Unit = {}
): Source {
    return (dir?.let(::File) ?: createTempDir(prefix = "local_git_repo")).let { directory ->
        val extendContext: Source.() -> Unit = {
            info["repo"] = repo
            info["file"] = file
            info["dir"] = directory.path
            info["branch"] = branch
        }
        try {
            if ((directory.list { _, name -> name == ".git" } ?: emptyArray()).isEmpty()) {
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
        } catch (ex: Exception) {
            when (ex) {
                is GitAPIException, is IOException, is SourceException -> {
                    if (optional) {
                        return EmptyMapSource.apply(extendContext)
                    } else {
                        throw ex
                    }
                }
                else -> throw ex
            }
        }
        fromFile(Paths.get(directory.path, file).toFile(), optional).apply(extendContext)
    }
}
