/**
 * Copyright 2013, 2014  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Version 0.94
 */
package org.scalaml.supervised.bayes.text

import org.scalaml.workflow.data.DocumentsSource
import TermsScore._
import org.scalaml.util.Counter
import scala.collection.mutable.HashMap
import scala.annotation.implicitNotFound
import scala.util.{Try, Success, Failure}
import DocumentsSource._
import org.scalaml.util.Display
import org.apache.log4j.Logger



	/**
	 * <p>Class to extract and score terms extracted from a set of news articles.</p>
	 * @constructor Instantiates a terms extractor and scoring class. [toDate] Function to convert a string date into a Long. [toWords] Function to extracts an array of a keywords from a line. [lexicon] Simple dictionary or map of tuples (words, root word) the root word is a lemme.
	 * @throws IllegalArgumentException if one of the class parameters is undefined
	 * @throws ImplicitNotFoundException if ordering is not defined prior the instantiation of this class
	 * 
	 *  @author Patrick Nicolas
	 *  @since April 6, 2014
	 *  @note Scala for Machine Learning 
	 */
@implicitNotFound("Ordering not explicitly defined for NaiveBayesTextScoring class")
class TermsScore[T <% Long](toDate: String =>T, 
		          			toWords: String => Array[String], 
		          		    lexicon: Map[String, String])(implicit val order: Ordering[T]) {
	
  require(toDate != null, "TermsScore Cannot score a text without an extractor for the release date")
  require(toWords != null, "TermsScore Cannot score a text without a word extractor")
  require(lexicon != null, "TermsScore Cannot score a text without a lexicon")
	
  private val logger = Logger.getLogger("NaiveBayesTextScoring")
   
   	/**
   	 * <p>Method to organize a corpus (set of documents) into a ordered sequence of map of
   	 * New articles tuples (data, weighted terms).</p>
   	 * @param corpus Corpus of news articles or documents
   	 * @throws IllegalArgumentException if the corpus is not defined
   	 * @return news articles (sequence of (date, weighted terms) map, ordered by date of release
   	 */
   import NewsArticles._
   def score(corpus: Corpus): Option[NewsArticles[T]] = {
  	  Try {
  	  	 val docs = rank(corpus)
  	  	 val cnts = docs.map( doc => (doc._1, count(doc._3)) )
  	     val totalCnts = cnts.map( _._2).foldLeft(new Counter[String])((s, cnt) => s ++ cnt) 
  	     val articles = NewsArticles[T]
  	  	 cnts.foreach(cnt => articles += (cnt._1, (cnt._2/totalCnts).toMap))
  	  	    
  	  	 articles
  	  } match {
  	  	 case Success(newsarticles) => Some(newsarticles)
  	  	 case Failure(e) => Display.error("TermsScore.score ", logger, e); None
  	  }
   }

  
  
   private[this] def count(term: String): Counter[String] = {
  	 require(term != null && term.length > 0, "TermsScore.count: Cannot count the number of words in undefined text")
   
  	 toWords(term).foldLeft(new Counter[String])((cnt, w) => 
  	  	   if( lexicon.contains(w)) cnt + lexicon(w) else cnt )
   }
   
   private[this] def rank(corpus: Corpus): CorpusType[T] = {
  	  require( corpus!= null && corpus.size > 0, "TermsScore.rank: Cannot order an undefined corpus of document")
   	  
  	  corpus.map(doc => (toDate(doc._1.trim), doc._2, doc._3)).sortWith( _._1 < _._1)
   }
}
   
   
object TermsScore {
    type CorpusType[T] = Array[(T, String, String)]
    def apply[T <% Long](tStamp: String =>T, words: String => Array[String], dict: Map[String, String])(implicit ordering: Ordering[T]): TermsScore[T] 
      = new TermsScore[T](tStamp, words, dict)
}


// ----------------------------  EOF ------------------------------------------