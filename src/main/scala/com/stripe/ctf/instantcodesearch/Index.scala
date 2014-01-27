package com.stripe.ctf.instantcodesearch

import java.io._

class Index(repoPath: String) {
  var files = List[String]()

  def path() = repoPath

  def addFile(file: String, text: String) {
    files = file :: files
  }

}
