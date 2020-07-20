/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package callStack.profiler

import callStack.profiler.AsyncProcess
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


@Slf4j
class AsyncProcessSpecification extends Specification{

    AsyncProcess asyncProcess
    void cleanup(){
        asyncProcess.stop()
    }

    def "Able to async process closure code"(){
        AtomicInteger count = new AtomicInteger()

        asyncProcess = new AsyncProcess()
        asyncProcess.start()
        when:
        asyncProcess.async {
            count.andIncrement
        }
        asyncProcess.async {
            count.andIncrement
        }
        asyncProcess.async {
            count.andIncrement
        }
        waitFor(count, 3)

        then:
        count.get() == 3
    }

    private void waitFor(AtomicInteger count, int numToWait) {
        int num = 0
        while (count.get() != numToWait && num < 10) {
            Thread.sleep(200)
            num++
        }
    }

    def "Async code may throw exceptions"(){
        AtomicInteger count = new AtomicInteger()

        asyncProcess = new AsyncProcess()
        asyncProcess.start()
        when:
        asyncProcess.async {
            if(true){throw new IllegalArgumentException("fail")}
            count.andIncrement
        }
        asyncProcess.async {
            count.andIncrement
        }
        asyncProcess.async {
            count.andIncrement
        }
        waitFor(count, 2)

        then:
        count.get() == 2
    }

    def "Execute real slow closure"(){
        AtomicInteger count = new AtomicInteger(0)

        asyncProcess = new AsyncProcess()
        asyncProcess.start()
        when:
        long start = System.currentTimeMillis()
        asyncProcess.async {
            Thread.sleep(5000)
            count.getAndIncrement()
        }

        long diff = System.currentTimeMillis()-start
        int num = 0
        while (count.get() != 1 && num < 10) {
            Thread.sleep(1000)
            num++
        }
        then:
        diff < 1000
        count.get() == 1
    }


    def "throw an exception if async queue is full"(){

        asyncProcess = new AsyncProcess(queueSize:2)
        asyncProcess.start()

        asyncProcess.async { Thread.sleep(50000) }
        asyncProcess.async { Thread.sleep(1) }

        Throwable t
        when:
        try {
            asyncProcess.async { Thread.sleep(50000) }
            asyncProcess.async { Thread.sleep(50000) }
        } catch (IllegalStateException e){
            e.printStackTrace()
            t = e
        }

        then:
        asyncProcess.stop()
        t.message == "Queue full"
    }


    def "support drop-if-full option"(){

        log.error("starting [support drop-if-full option] test")
        AtomicInteger count = new AtomicInteger(0)

        asyncProcess = new AsyncProcess(queueSize:1, dropIfFull:true)
        asyncProcess.start()

        log.error("before async 1")
        boolean res1 =asyncProcess.async {
            TimeUnit.SECONDS.sleep(6)
        }
        log.error("async 1 res: ${res1}")

        log.error("before async 2")
        boolean res2 = asyncProcess.async {
        }
        log.error("async 2 res: ${res2}")

        log.error("before async 3")
        boolean res3 = asyncProcess.async {
        }
        log.error("async 3 res: ${res3}")
        when:

        log.error("before async 4")
        boolean kept = asyncProcess.async {
            count.incrementAndGet()
        }
        log.error("async 4 res: ${kept}")

        TimeUnit.SECONDS.sleep(10)

        then:
        !kept
        count.get().intValue() == 0
    }

}
