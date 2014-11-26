package com.stripe.ctf.instantcodesearch

import java.io._
import java.nio.file._

import com.twitter.concurrent.Broker

import scala.collection.mutable.HashMap

abstract class SearchResult
case class Match(path: String, line: Int) extends SearchResult
case class Done() extends SearchResult

class Searcher(index : Index)  {
  val root = FileSystems.getDefault().getPath(index.path)
  val cache = getFileCache(index.files)

  def search(needle : String, b : Broker[SearchResult]) = {
    for (path <- index.files) {
      for (m <- tryCache(path, needle)) {
        b !! m
      }
    }

    b !! new Done()
  }

  def tryPath(path: String, needle: String) : Iterable[SearchResult] = {
    try {
      val text : String = slurp(root.resolve(path))
      if (text.contains(needle)) {
        var line = 0
        return text.split("\n").zipWithIndex.
          filter { case (l,n) => l.contains(needle) }.
          map { case (l,n) => new Match(path, n+1) }
      }
    } catch {
      case e: IOException => {
        return Nil
      }
    }

    return Nil
  }

  def tryCache(path: String, needle: String) : Iterable[SearchResult] = {
    val lines = cache(path)
    lines.filter{ case(l,n) => l.contains(needle) }.
    map { case(l,n) => new Match(path, n+1) }
  }

  def readIndex(path: String) : Index = {
    new ObjectInputStream(new FileInputStream(new File(path))).readObject.asInstanceOf[Index]
  }

  def getFileLines(path: String) : Array[(String, Int)] = {
    val text : String = slurp(root.resolve(path))
    text.split("\n").zipWithIndex
  }

  def getFileCache(files: List[String]) : HashMap[String, Array[(String, Int)]] = {
    val hash = new HashMap[String, Array[(String,Int)]]
    files.foreach { file =>
      hash += (file -> getFileLines(file))
    }
    hash
  }
}
