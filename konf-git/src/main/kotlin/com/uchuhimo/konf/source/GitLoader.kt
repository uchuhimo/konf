/*
 * Copyright 2017-2021 the original author or authors.
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

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.tempDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Constants
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Returns a child config containing values from a specified git repository.
 *
 * @param repo git repository
 * @param file file in the git repository
 * @param dir local directory of the git repository
 * @param branch the initial branch
 * @param optional whether the source is optional
 * @return a child config containing values from a specified git repository
 */
fun Loader.git(
    repo: String,
    file: String,
    dir: String? = null,
    branch: String = Constants.HEAD,
    optional: Boolean = this.optional
): Config =
    config.withSource(provider.git(repo, file, dir, branch, optional))

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
 * @param context context of the coroutine. The default value is [Dispatchers.Default].
 * @param optional whether the source is optional
 * @param onLoad function invoked after the updated git file is loaded
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
    optional: Boolean = this.optional,
    onLoad: ((config: Config, source: Source) -> Unit)? = null
): Config {
    return (dir ?: tempDirectory(prefix = "local_git_repo").path).let { directory ->
        provider.git(repo, file, directory, branch, optional).let { source ->
            config.withLoadTrigger("watch ${source.description}") { newConfig, load ->
                newConfig.lock {
                    load(source)
                }
                onLoad?.invoke(newConfig, source)
                GlobalScope.launch(context) {
                    while (true) {
                        delay(unit.toMillis(period))
                        val newSource = provider.git(repo, file, directory, branch, optional)
                        newConfig.lock {
                            newConfig.clear()
                            load(newSource)
                        }
                        onLoad?.invoke(newConfig, newSource)
                    }
                }
            }.withLayer()
        }
    }
}
