package com.stripe.ctf.instantcodesearch

import java.io._
import java.nio.file._

import com.twitter.concurrent.Broker

import scala.collection.concurrent.TrieMap

abstract class SearchResult
case class Match(path: String, line: Int) extends SearchResult
case class Done() extends SearchResult

class Searcher(index : Index)  {
  val root = FileSystems.getDefault().getPath(index.path)
  val cache = getFileCache(index.files)
  val invertedIndex = getInvertedIndex(index.files)
  println("finished indexing")

  def search(needle : String, b : Broker[SearchResult]) = {
    for (m <- tryInvertedIndex(needle)) {
      b !! m
    }

    b !! new Done()
  }

  def tryInvertedIndex(needle: String) : Iterable[SearchResult] = {
    //val needle_words = needle.split(" ")
    //val results = needle_words.map { word => invertedIndex(word) }
    //results.flatten
    println("going to return inverted index results for " + needle)
    val result = invertedIndex.getOrElse(needle, Array[SearchResult]())
    println("got result")
    result
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

  def getFileCache(files: List[String]) : TrieMap[String, Array[(String, Int)]] = {
    val hash = new TrieMap[String, Array[(String,Int)]]
    files.foreach { file =>
      hash += (file -> getFileLines(file))
    }
    hash
  }

  def getInvertedIndex(files: List[String]) : TrieMap[String, Array[SearchResult]] = {
    val hash = new TrieMap[String, Array[SearchResult]]
    files.foreach { file =>
      val lines = cache(file)
      lines.foreach { line =>
	val words = line._1.split(" ")
	words.foreach { word =>
	  val results = hash.getOrElse(word, Array[SearchResult]())
	  val updatedResults = results :+ Match(file, line._2 + 1)
	  hash += (word -> updatedResults)
	}
      }
    }
    hash
  }
}
