/*
 * Copyright 2015 Toshiyuki Takahashi
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
package com.github.tototoshi.sbt.buildfileswatcher

import sbt._
import Keys._
import scala.collection.mutable.{ Map => MutableMap }

object Plugin extends sbt.Plugin {

  private val buildFilesHashOnLoad = SettingKey[Map[File, Seq[Byte]]]("buildFilesHashOnLoad")
  val messageOnBuildFilesChanged = SettingKey[State => String]("messageOnBuildFilesChanged")

  private def getBuildFiles(base: File): Seq[File] = {
    ((base * "*.sbt") +++ ((base / "project") ** ("*.scala" | "*.sbt"))).get
  }

  private def hash(files: Seq[File]): Map[File, Seq[Byte]] = files.map {
    f => f -> collection.mutable.WrappedArray.make[Byte](Hash(f))
  }.toMap

  private def listBuildFiles(state: State): Seq[File] = {
    val structure = Project.structure(state)
    (for {
      proj <- structure.allProjects
      file <- getBuildFiles(proj.base)
    } yield file).distinct
  }

  val messageOnBuildFilesChangedSetting: Setting[_] =
    messageOnBuildFilesChanged := { state: State =>
      val files = listBuildFiles(state)
      if (buildFilesHashOnLoad.value != hash(files)) {
        scala.Console.RED + "Build files changed. Please reload." + scala.Console.RESET + "\n"
      } else {
        ""
      }
    }

  val showMessageOnBuildFilesChanged: Setting[_] =
    shellPrompt := { state =>
      messageOnBuildFilesChanged.value(state) + "> "
    }

  override lazy val settings = Seq(
    messageOnBuildFilesChangedSetting
  )
}
