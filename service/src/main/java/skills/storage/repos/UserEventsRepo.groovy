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
package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.DayCountItem
import skills.storage.model.EventCount
import skills.storage.model.EventType
import skills.storage.model.UserEvent
import skills.storage.model.WeekCount

import javax.persistence.QueryHint
import java.util.stream.Stream

@CompileStatic
interface UserEventsRepo extends CrudRepository<UserEvent, Integer> {

    @Nullable
    @Query(value="""
        select new skills.storage.model.WeekCount(ue.weekNumber, sum(ue.count)) from UserEvent ue
        where ue.eventTime >= :start AND ue.skillRefId = :skillRefId
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getEventCountForSkillGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Nullable
    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.eventTime, sum(ue.count)) from UserEvent ue
        where ue.eventTime >= :start AND ue.skillRefId = :skillRefId AND ue.eventType = :type 
        group by ue.eventTime
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getEventCountForSkill(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)

    @Nullable
    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.eventTime, count(ue.userId)) from UserEvent ue
        where ue.eventTime > :start AND ue.skillRefId = :skillRefId AND ue.eventType = :type 
        group by ue.eventTime
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getDistinctUserCountForSkill(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)

    @Nullable
    @Query(value="""
        select new skills.storage.model.WeekCount(ue.weekNumber, count(distinct ue.userId)) from UserEvent ue
        where ue.eventTime >= :start AND ue.skillRefId = :skillRefId
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getDistinctUserCountForSkillGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.eventTime, sum(ue.count)) from UserEvent ue
        where ue.eventTime > :start AND ue.eventType = :type AND 
        ue.skillRefId in (SELECT child.id FROM SkillRelDef where parent.id = :skillRefId)
        group by ue.eventTime
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getEventCountForSubject(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)

    @Nullable
    @Query(value="""
        select new skills.storage.model.WeekCount(ue.weekNumber, sum(ue.count)) from UserEvent ue
        where ue.eventTime >= :start AND ue.skillRefId in (SELECT child.id FROM SkillRelDef where parent.id = :skillRefId)
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getEventCountForSubjectGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)


    @Query(value="""
        select new skills.storage.model.WeekCount(ue.weekNumber, count(distinct ue.userId)) from UserEvent ue
        where ue.eventTime >= :start AND
        ue.skillRefId in (SELECT child.id FROM SkillRelDef where parent.id = :skillRefId)
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getDistinctUserCountForSubjectGroupedByWeek(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Query(value="""
        select new skills.storage.model.EventCount(ue.eventTime, count(distinct ue.userId), ue.eventType) from UserEvent ue
        where ue.eventTime > :start AND
        ue.skillRefId in (SELECT child.id FROM SkillRelDef where parent.id = :skillRefId)
        group by ue.eventTime, ue.eventType
        order by ue.eventTime desc
    """)
    Stream<EventCount> getDistinctUserCountForSubject(@Param("skillRefId") Integer skillRefId, @Param("start") Date start)

    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.eventTime, count(distinct ue.userId)) from UserEvent ue
        where ue.eventTime > :start AND ue.eventType = :type AND 
        ue.skillRefId in (SELECT child.id FROM SkillRelDef where parent.id = :skillRefId)
        group by ue.eventTime
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getDistinctUserCountForSubject(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") EventType type)


    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.eventTime, sum(ue.count)) from UserEvent ue
        where ue.eventTime > :start AND ue.eventType = :type AND 
        ue.skillRefId in (SELECT sd.id FROM SkillDef sd WHERE sd.projectId = :projectId AND sd.type = 'Skill')   
        group by ue.eventTime 
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getEventCountForProject(@Param("projectId") String projectId, @Param("start") Date start, @Param("type") EventType type)

    @Query(value="""
        select new skills.storage.model.WeekCount(ue.weekNumber, sum(ue.count)) from UserEvent ue
        where ue.eventTime >= :start AND
        ue.skillRefId in (SELECT sd.id FROM SkillDef sd WHERE sd.projectId = :projectId AND sd.type = 'Skill')   
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getEventCountForProjectGroupedByWeek(@Param("projectId") String projectId, @Param("start") Date start)

    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.projectId, ue.eventTime, sum(ue.count)) from UserEvent ue
        where ue.eventTime > :start AND ue.eventType = :type AND ue.userId = :userId AND ue.projectId in (:projectIds)
        group by ue.projectId, ue.eventTime 
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getEventCountForUser(@Param("userId") String userId, @Param("start") Date start, @Param("type") EventType type, @Param("projectIds") List<String> projectIds)

    @Query(value="""
        select new skills.storage.model.WeekCount(ue.projectId, ue.weekNumber, sum(ue.count)) from UserEvent ue
        where ue.eventTime >= :start AND ue.userId = :userId AND ue.projectId in (:projectIds)
        group by ue.projectId, ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getEventCountForUserGroupedByWeek(@Param("userId") String userId, @Param("start") Date start, @Param("projectIds") List<String> projectIds)


    @Query(value="""
        select new skills.storage.model.DayCountItem(ue.eventTime, count(distinct ue.userId)) from UserEvent ue
        where ue.eventTime > :start AND ue.eventType = :type AND 
        ue.skillRefId in (SELECT sd.id FROM SkillDef sd WHERE sd.projectId = :projectId AND sd.type = 'Skill')   
        group by ue.eventTime 
        order by ue.eventTime desc
    """)
    Stream<DayCountItem> getDistinctUserCountForProject(@Param("projectId") String projectId, @Param("start") Date start, @Param("type") EventType type)

    @Query(value="""
        select new skills.storage.model.EventCount(ue.eventTime, count(distinct ue.userId), ue.eventType) from UserEvent ue
        where ue.eventTime > :start AND
        ue.skillRefId in (SELECT sd.id FROM SkillDef sd WHERE sd.projectId = :projectId AND sd.type = 'Skill')   
        group by ue.eventTime, ue.eventType
        order by ue.eventTime desc
    """)
    Stream<EventCount> getDistinctUserCountForProject(@Param("projectId") String projectId, @Param("start") Date start)

    @Query(value="""
        select new skills.storage.model.WeekCount(ue.weekNumber, count(distinct ue.userId)) from UserEvent ue
        where ue.eventTime >= :start AND
        ue.skillRefId in (SELECT sd.id FROM SkillDef sd WHERE sd.projectId = :projectId AND sd.type = 'Skill')   
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCount> getDistinctUserCountForProjectGroupedByWeek(@Param("projectId") String projectId, @Param("start") Date start)

    @Nullable
    Stream<UserEvent> findAllBySkillRefIdAndEventType(Integer skillRefId, EventType type)

    @QueryHints(value = [
        @QueryHint(name = "org.hibernate.cacheable", value = "false"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true")
    ])
    @Nullable
    Stream<UserEvent> findAllByEventTypeAndEventTimeLessThan(EventType type, Date start)

    void deleteByEventTypeAndEventTimeLessThan(EventType type, Date start)


}