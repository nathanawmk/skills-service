/**
 * Copyright 2021 SkillTree
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
package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec

class CachingSpec extends DefaultIntSpec{

    @Autowired
    ResourceLoader resourceLoader

    def "favicon should be cached" () {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/skilltree.ico", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "max-age=7776000, must-revalidate, private"
    }

    def "index.html must never be cached - access with /" () {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }

    def "index.html must never be cached - access with /request-root-account" () {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/request-root-account", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }

    def "index.html must never be cached - access with /skills-login" () {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/skills-login", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }


    def "js resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/js/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/js/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=7776000, must-revalidate, private"
        }

        then:
        true
    }

    def "img resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/img/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/img/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=1209600, must-revalidate, private"
        }

        then:
        true
    }

    def "fonts resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/fonts/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/fonts/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=1209600, must-revalidate, private"
        }

        then:
        true
    }

    def "css resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/css/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/css/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=7776000, must-revalidate, private"
        }

        then:
        true
    }

    def "clientPortal index page should not be cached"() {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("static/clientPortal/index.html", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }

    def "clientPortal js resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/clientPortal/js/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/clientPortal/js/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=7776000, must-revalidate, private"
        }

        then:
        true
    }

    def "clientPortal img resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/clientPortal/img/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/clientPortal/img/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=1209600, must-revalidate, private"
        }

        then:
        true
    }

    def "clientPortal fonts resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/clientPortal/fonts/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/clientPortal/fonts/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=1209600, must-revalidate, private"
        }

        then:
        true
    }

    def "clientPortal css resources should be cached"() {
        when:

        getFileNamesFromClasspath("/public/static/clientPortal/css/**").each {
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/static/clientPortal/css/${it}", [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=7776000, must-revalidate, private"
        }

        then:
        true
    }


    private getFileNamesFromClasspath(String classpathPath) {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath:${classpathPath}").collect {
            it.filename
        }
    }


}
