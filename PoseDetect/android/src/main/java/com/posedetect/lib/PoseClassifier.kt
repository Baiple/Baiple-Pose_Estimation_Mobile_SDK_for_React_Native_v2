/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package com.posedetect.lib

import android.content.Context
import android.graphics.PointF
import org.tensorflow.lite.Interpreter
//import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.support.common.FileUtil
import kotlin.math.*

class PoseClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {
    private val input = interpreter.getInputTensor(0).shape()
    private val output = interpreter.getOutputTensor(0).shape()

    companion object {
        private const val MODEL_FILENAME = "classifier.tflite"
        private const val LABELS_FILENAME = "labels.txt"
        private const val CPU_NUM_THREADS = 4

        fun create(context: Context): PoseClassifier {
            val options = Interpreter.Options().apply {
                setNumThreads(CPU_NUM_THREADS)
            }
            return PoseClassifier(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context, MODEL_FILENAME
                    ), options
                ),
                FileUtil.loadLabels(context, LABELS_FILENAME)
            )
        }
    }

    private fun sqrtNorm(x:Float, y:Float): Float {
      return sqrt((x*x + y*y))
    }
    /*
    자동 촬영을 위한 adress 각도를 찾는 함수.
     */
    private fun poseAngle(x1:Float, y1:Float, x2:Float, y2:Float): Double {
      var c = (x1*x2 + y1*y2)/(sqrtNorm(x1,y1)*sqrtNorm(x2,y2))
      return acos(c)*180/3.1415926536
    }

    fun classify(person: Person?): List<Pair<String, Float>> {
        // Preprocess the pose estimation result to a flat array
        val inputVector = FloatArray(input[1])
        person?.keyPoints?.forEachIndexed { index, keyPoint ->
            inputVector[index * 3] = keyPoint.coordinate.y
            inputVector[index * 3 + 1] = keyPoint.coordinate.x
            inputVector[index * 3 + 2] = keyPoint.score
        }

        //println(person?.keyPoints?.slice(0..0)?.javaClass)
//        println("${person?.keyPoints?.get(0)?.coordinate?.x}")
//        println(person?.keyPoints?.slice(0..0)?.indexOf(element = KeyPoint(co)))
        var y0 = person?.keyPoints?.get(0)?.coordinate?.y
        var x5 = person?.keyPoints?.get(5)?.coordinate?.x
        var y5 = person?.keyPoints?.get(5)?.coordinate?.y
        var x6 = person?.keyPoints?.get(6)?.coordinate?.x
        var y6 = person?.keyPoints?.get(6)?.coordinate?.y
        var y9 = person?.keyPoints?.get(9)?.coordinate?.y
        var x11 = person?.keyPoints?.get(11)?.coordinate?.x
        var y11 = person?.keyPoints?.get(11)?.coordinate?.y
        var x12 = person?.keyPoints?.get(12)?.coordinate?.x
        var y12 = person?.keyPoints?.get(12)?.coordinate?.y
        var y15 = person?.keyPoints?.get(15)?.coordinate?.y
        var y16 = person?.keyPoints?.get(16)?.coordinate?.y
        var neck_x = (x5?.plus(x6!!))?.div(2)
        var neck_y = (y5?.plus(y6!!))?.div(2)
        var pelvis_x = (x11?.plus(x12!!))?.div(2)
        var pelvis_y = (y11?.plus(y12!!))?.div(2)
        var A_x = neck_x?.minus(pelvis_x!!)!!
        var A_y = neck_y?.minus(pelvis_y!!)!!
        var B_x = 0 - pelvis_x!!
        var B_y = 1 - pelvis_y!!

        var spine = poseAngle(A_x,A_y,B_x,B_y)
//        println("spine type : ${spine.javaClass}")
//        println("spine : ${spine}")
        var height: Float = 0F
        if (y16 != null) {
          if(y16 > y15!!)
            height = y16 - y0!!
          else
            height = y15 - y0!!
        }
        var hand_y_r = y9?.minus(y0!!)
        var hand_proportion = hand_y_r?.div(height)
        var pose_status:String

//        println("***************************************************")
        // Postprocess the model output to human readable class names
        val outputTensor = FloatArray(output[1])
        interpreter.run(arrayOf(inputVector), arrayOf(outputTensor))
        val output = mutableListOf<Pair<String, Float>>()
        outputTensor.forEachIndexed { index, score ->
//          output.add(Pair(labels[index], score))
//          println("==================================")
//          println("***** labels[index]: "+labels[index] +": [score]:" + score)
//          println("==================================")
          if (50 < spine && spine < 70 && 0.4 < hand_proportion!! && hand_proportion < 0.5) {
//            println("adress")
            pose_status = "adress"
            output.add(Pair(pose_status, 1.0f))
          }
        }

        return output
    }

    fun close() {
        interpreter.close()
    }
}
