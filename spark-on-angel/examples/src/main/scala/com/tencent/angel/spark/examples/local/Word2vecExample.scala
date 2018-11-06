/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.tencent.angel.spark.examples.local

import com.tencent.angel.conf.AngelConf
import com.tencent.angel.ps.storage.matrix.PartitionSourceArray

import scala.util.Random
import com.tencent.angel.spark.context.PSContext
import com.tencent.angel.spark.ml.embedding.Param
import com.tencent.angel.spark.ml.embedding.word2vec.Word2VecModel
import com.tencent.angel.spark.ml.feature.{Features, SubSampling}
import org.apache.spark.{SparkConf, SparkContext}

object Word2vecExample {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    conf.setMaster("local[1]")
    conf.setAppName("Word2vec example")
    conf.set("spark.ps.model", "LOCAL")
    conf.set("spark.ps.jars", "")
    conf.set("spark.ps.instances", "1")
    conf.set("spark.ps.cores", "1")

    conf.set(AngelConf.ANGEL_PS_PARTITION_SOURCE_CLASS, classOf[PartitionSourceArray].getName)

    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")
    PSContext.getOrCreate(sc)

    val input = "data/text8/text8.split.head"
    val output = "model/"

    val data = sc.textFile(input)
    data.cache()

    val (corpus, denseToString) = Features.corpusStringToInt(sc.textFile(input))
    val docs = SubSampling.sampling(corpus).repartition(2)

    docs.cache()
    docs.count()

    data.unpersist()

    val numDocs = docs.count()
    val maxWordId = docs.map(_.max).max().toLong + 1
    val numTokens = docs.map(_.length).sum().toLong
    val maxLength = docs.map(_.length).max()

    println(s"numDocs=$numDocs maxWordId=$maxWordId numTokens=$numTokens")

    val param = new Param()
    param.setLearningRate(0.1f)
    param.setEmbeddingDim(100)
    param.setWindowSize(6)
    param.setBatchSize(128)
    param.setSeed(Random.nextInt())
    param.setNumPSPart(Some(2))
    param.setNumEpoch(10)
    param.setNegSample(5)
    param.setMaxIndex(maxWordId)
    param.setMaxLength(maxLength)

    val model = new Word2VecModel(param)
    model.train(docs, param)

//    model.save(output + "embedding", 0)
//    denseToString.map(f => s"${f._1}:${f._2}").saveAsTextFile(output + "mapping")

    PSContext.stop()
    sc.stop()
  }

}
