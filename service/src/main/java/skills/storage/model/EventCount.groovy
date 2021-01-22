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
package skills.storage.model

class EventCount {

    Date start
    Date stop
    Long count
    EventType eventType

    EventCount(Date start, Date stop, Long count, EventType eventType) {
        this.start = start
        this.stop = stop
        this.count = count
        this.eventType = eventType
    }

    EventCount(Date start, Date stop, Long count, String eventType) {
        this.start = start
        this.stop = stop
        this.count = count
        this.eventType = EventType.valueOf(eventType)
    }
}
