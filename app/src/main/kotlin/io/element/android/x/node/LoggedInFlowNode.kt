/*
 * Copyright (c) 2023 New Vector Ltd
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

package io.element.android.x.node

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.Coil
import com.bumble.appyx.core.composable.Children
import com.bumble.appyx.core.lifecycle.subscribe
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.node.ParentNode
import com.bumble.appyx.core.node.node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.operation.push
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.features.preferences.PreferencesFlowNode
import io.element.android.features.roomlist.api.RoomListEntryPoint
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.animation.rememberDefaultTransitionHandler
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.architecture.createNode
import io.element.android.libraries.architecture.nodeInputs
import io.element.android.libraries.architecture.nodeInputsProvider
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.DaggerComponentOwner
import io.element.android.libraries.matrix.MatrixClient
import io.element.android.libraries.matrix.core.RoomId
import io.element.android.libraries.matrix.ui.di.MatrixUIBindings
import io.element.android.x.di.SessionComponent
import kotlinx.parcelize.Parcelize

@ContributesNode(AppScope::class)
class LoggedInFlowNode(
    buildContext: BuildContext,
    plugins: List<Plugin>,
    private val backstack: BackStack<NavTarget>,
    private val roomListEntryPoint: RoomListEntryPoint,
) : ParentNode<LoggedInFlowNode.NavTarget>(
    navModel = backstack,
    buildContext = buildContext,
    plugins = plugins
), DaggerComponentOwner {

    @AssistedInject
    constructor(
        @Assisted buildContext: BuildContext,
        @Assisted plugins: List<Plugin>,
        roomListEntryPoint: RoomListEntryPoint,
    ) : this(
        buildContext = buildContext,
        plugins = plugins,
        roomListEntryPoint = roomListEntryPoint,
        backstack = BackStack(
            initialElement = NavTarget.RoomList,
            savedStateMap = buildContext.savedStateMap,
        )
    )

    interface Callback : Plugin {
        fun onOpenBugReport()
    }

    data class Inputs(
        val matrixClient: MatrixClient
    ) : NodeInputs

    private val inputs: Inputs by nodeInputs()

    override val daggerComponent: Any by lazy {
        parent!!.bindings<SessionComponent.ParentBindings>().sessionComponentBuilder().client(inputs.matrixClient).build()
    }

    override fun onBuilt() {
        super.onBuilt()
        lifecycle.subscribe(
            onCreate = {
                val imageLoaderFactory = bindings<MatrixUIBindings>().loggedInImageLoaderFactory()
                Coil.setImageLoader(imageLoaderFactory)
                inputs.matrixClient.startSync()
            },
            onDestroy = {
                val imageLoaderFactory = bindings<MatrixUIBindings>().notLoggedInImageLoaderFactory()
                Coil.setImageLoader(imageLoaderFactory)
            }
        )
    }

    sealed interface NavTarget : Parcelable {
        @Parcelize
        object RoomList : NavTarget

        @Parcelize
        data class Room(val roomId: RoomId) : NavTarget

        @Parcelize
        object Settings : NavTarget
    }

    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node {
        return when (navTarget) {
            NavTarget.RoomList -> {
                val callback = object : RoomListEntryPoint.Callback {
                    override fun onRoomClicked(roomId: RoomId) {
                        backstack.push(NavTarget.Room(roomId))
                    }

                    override fun onSettingsClicked() {
                        backstack.push(NavTarget.Settings)
                    }
                }
                roomListEntryPoint.node(this, buildContext, plugins = listOf(callback))
            }
            is NavTarget.Room -> {
                val room = inputs.matrixClient.getRoom(roomId = navTarget.roomId)
                if (room == null) {
                    // TODO CREATE UNKNOWN ROOM NODE
                    node(buildContext) {
                        Box(modifier = it.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Unknown room with id = ${navTarget.roomId}")
                        }
                    }
                } else {
                    val inputsProvider = nodeInputsProvider(RoomFlowNode.Inputs(room))
                    createNode<RoomFlowNode>(buildContext, plugins = listOf(inputsProvider))
                }
            }
            NavTarget.Settings -> {
                val callback = object : PreferencesFlowNode.Callback {
                    override fun onOpenBugReport() {
                        plugins<Callback>().forEach { it.onOpenBugReport() }
                    }
                }
                createNode<PreferencesFlowNode>(buildContext, plugins = listOf(callback))
            }
        }
    }

    @Composable
    override fun View(modifier: Modifier) {
        Children(
            navModel = backstack,
            modifier = modifier,
            // Animate navigation to settings and to a room
            transitionHandler = rememberDefaultTransitionHandler(),
        )
    }
}
