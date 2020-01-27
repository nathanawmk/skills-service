package skills.storage.repos.nativeSql

import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import skills.storage.model.SkillDef

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

@Conditional(DBConditions.H2)
@Service
class H2NativeRepo implements NativeQueriesRepo {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    void decrementPointsForDeletedSkill(String projectId, String deletedSkillId, String parentSubjectSkillId) {
        String q = '''
        merge into USER_POINTS(id, points) key(id)
        SELECT b.id, b.points - a.points as points
        FROM user_points a, user_points b
        WHERE a.user_id = b.user_id
          and (a.day = b.day OR (a.day is null and b.day is null))
          and a.skill_id = :deletedSkillId
          and (b.skill_id = :parentSubjectSkillId or b.skill_id is null)
          and b.project_id = :projectId
          and a.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("parentSubjectSkillId", parentSubjectSkillId)
        query.setParameter("deletedSkillId", deletedSkillId)
        query.executeUpdate();
    }

    @Override
    void updateOverallScoresBySummingUpAllChildSubjects(String projectId, SkillDef.ContainerType subjectType) {
        String q = '''
        merge into USER_POINTS (id, points) key (id)
    select points.id, sum.sumPoints
    from (select user_id sumUserId, day sumDay, SUM(pointsInner.points) sumPoints
          from user_points pointsInner
                   join skill_definition definition
                        on pointsInner.project_id = definition.project_id and
                           pointsInner.skill_id = definition.skill_id and
                           definition.type = :subjectType
          where pointsInner.project_id = :projectId
            and definition.project_id = :projectId
          group by user_id, day) sum,
         user_points points
    where sum.sumUserId = points.user_id
      and (sum.sumDay = points.day OR (sum.sumDay is null and points.day is null))
      and points.skill_id is null
      and points.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("subjectType", subjectType.toString())
        query.executeUpdate();
    }

    @Override
    List<GraphRelWithAchievement> getDependencyGraphWithAchievedIndicator(String projectId, String skillId, String userId){
        String q = '''
            WITH RECURSIVE skill_deps_path(parentProjectId, parentSkillId, parentId, parentName, childProjectId, childSkillId, childId, childName) AS (
              select sd.project_id as parentProjectId, sd.skill_id as parentSkillId, sd.id as parentId, sd.name as parentName,
                     sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName
              from skill_definition sd,
                   skill_relationship_definition srd,
                   skill_definition sd1
              where sd.id = srd.parent_ref_id
                and sd1.id = srd.child_ref_id
                and srd.type = 'Dependence'
                and sd.project_id=:projectId and sd.skill_id=:skillId 
              UNION ALL
              select skill_deps_path.childProjectId as parentProjectId, skill_deps_path.childSkillId as parentSkillId, skill_deps_path.childId as parentId, skill_deps_path.childName as parentName,
                     sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName
              from  skill_deps_path,
                   skill_relationship_definition srd,
                   skill_definition sd1
              where skill_deps_path.childId = srd.parent_ref_id
                and sd1.id = srd.child_ref_id
                and srd.type = 'Dependence'
                and skill_deps_path.childProjectId=:projectId
            )
            select pd.project_id as parentProjectId, pd.name as parentProjectName, skill_deps_path.parentId, skill_deps_path.parentSkillId, skill_deps_path.parentName,
                   skill_deps_path.childProjectId, pd1.name as childProjectName, skill_deps_path.childId, skill_deps_path.childSkillId, skill_deps_path.childName,
                   ua.id as achievementId
            from skill_deps_path
              join project_definition pd on skill_deps_path.parentProjectId = pd.project_id
              join project_definition pd1 on skill_deps_path.childProjectId = pd1.project_id
              left join user_achievement ua
                ON ua.skill_ref_id = skill_deps_path.childId AND ua.user_id=:userId
         '''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("userId", userId)

        List resList = query.getResultList().collect {
            new GraphRelWithAchievement(
                    parentProjectId: it[0],
                    parentProjectName: it[1],
                    parentId: it[2],
                    parentSkillId: it[3],
                    parentName: it[4],

                    childProjectId: it[5],
                    childProjectName: it[6],
                    childId: it[7],
                    childSkillId: it[8],
                    childName: it[9],
                    achievementId: it[10]
            )
        }
        return resList
    }

    void updatePointTotalsForSkill(String projectId, String subjectId, String skillId, int incrementDelta){
        String eventCountSql = '''
            SELECT 
                user_id, COUNT(id) eventCount
            FROM 
                user_performed_skill
            WHERE 
                skill_id = :skillId
                AND project_id = :projectId
            GROUP BY 
                user_id
           '''

        Query query = entityManager.createNativeQuery(eventCountSql);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        List<PerformedSkillEventCount> eventCounts = query.getResultList().collect{
            new PerformedSkillEventCount(userId: it[0], eventCount: it[1])
        }


        String updateSql = '''
            UPDATE
                user_points points
            SET
                points = points + (:eventCount * :incrementDelta)
            WHERE
                points.user_id = :userId
                AND points.day IS NULL
                AND points.project_id=:projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''

        eventCounts?.each {
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("eventCount", it.eventCount)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.setParameter("subjectId", subjectId)
            updateQ.setParameter("incrementDelta", incrementDelta)
            updateQ.executeUpdate()
        }
    }

    void updatePointHistoryForSkill(String projectId, String subjectId, String skillId, int incrementDelta){
        String eventCountSql = '''
            SELECT
                user_id, COUNT(id) eventCount, FORMATDATETIME(performed_on,'yyyy-MM-dd') performedOn
            FROM
                user_performed_skill
            WHERE
                skill_id = :skillId AND project_id = :projectId
            GROUP BY
                user_id, FORMATDATETIME(performed_on,'yyyy-MM-dd')
           '''

        Query eventCountQuery = entityManager.createNativeQuery(eventCountSql);
        eventCountQuery.setParameter("projectId", projectId);
        eventCountQuery.setParameter("skillId", skillId)
        List<PerformedSkillEventCount> eventCounts = eventCountQuery.getResultList().collect{
            new PerformedSkillEventCount(userId: it[0], eventCount: it[1], performedOn: it[2])
        }

        String updateSql = '''
            UPDATE 
                user_points points
            SET 
                points = points + (:eventCount * :incrementDelta) 
            WHERE
                points.user_id = :userId
                AND points.day = :performedOn
                AND points.project_id = :projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''
        eventCounts?.each{
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("eventCount", it.eventCount)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.setParameter("subjectId", subjectId)
            updateQ.setParameter("performedOn", it.performedOn)
            updateQ.setParameter("incrementDelta", incrementDelta)
            updateQ.executeUpdate()
        }

    }

    private static class PerformedSkillEventCount{
        String userId
        int eventCount
        String performedOn
    }

    @Override
    void updatePointTotalWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int numOccurrences) {
        String q = '''
            WITH
                eventsRes AS (
                    SELECT rank_filter.user_id userId, count(rank_filter.id) count
                    FROM (
                        SELECT user_performed_skill.id, user_performed_skill.user_id
                            rank() OVER (
                                PARTITION BY user_id
                                ORDER BY created DESC
                            )
                        FROM user_performed_skill
                        where project_id = :projectId and skill_id = :skillId
                    ) rank_filter
                    WHERE RANK > :numOccurrences
                    group by userId
                )
            UPDATE 
                user_points points
            SET 
                points = points - (eventsRes.eventCount * :pointIncrement) 
            FROM
                eventsRes
            WHERE
                eventsRes.user_id = points.user_id
                AND points.day IS NULL 
                AND points.project_id=:projectId 
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("subjectId", subjectId)
        query.setParameter("numOccurrences", numOccurrences)
        query.setParameter("pointIncrement", pointIncrement)
        query.executeUpdate()
    }

    @Override
    void updatePointHistoryWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int numOccurrences) {
        String q = '''
            WITH
                eventsRes AS (
                    SELECT rank_filter.user_id userId, DATE(performed_on) performedOn, count(rank_filter.id) count
                    FROM (
                        SELECT user_performed_skill.id, user_performed_skill.user_id, user_performed_skill.performed_on,
                            rank() OVER (
                                PARTITION BY user_id
                                ORDER BY created DESC
                            )
                        FROM user_performed_skill
                        where project_id = :projectId and skill_id = :skillId
                    ) rank_filter
                    WHERE RANK > :numOccurrences
                    group by userId, DATE(performed_on)
                )
            UPDATE 
                user_points points
            SET 
                points = points - (eventsRes.eventCount * :pointIncrement) 
            FROM
                eventsRes
            WHERE
                eventsRes.user_id = points.user_id
                AND eventsRes.performedOn = points.day
                AND points.project_id = :projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("subjectId", subjectId)
        query.setParameter("numOccurrences", numOccurrences)
        query.setParameter("pointIncrement", pointIncrement)
        query.executeUpdate()
    }

    @Override
    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep){
        String q = '''
            DELETE from user_performed_skill ups
            USING (SELECT rank_filter.id FROM (
                SELECT user_performed_skill.id, user_performed_skill.created,
                       rank() OVER (
                           PARTITION BY user_id
                           ORDER BY created DESC
                           )
                FROM user_performed_skill where project_id = :projectId and skill_id = :skillId
            ) rank_filter WHERE RANK > :numEventsToKeep) as idsToRemove
            WHERE idsToRemove.id = ups.id;'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("numEventsToKeep", numEventsToKeep)
        query.executeUpdate()
    }

    @Override
    void insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(String projectId, String skillId, Integer skillRefId, int numOfOccurrences) {
        String q = '''
            INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved)
            SELECT eventsByUserId.user_id, :projectId, :skillId, :skillRefId, -1
            FROM (
                SELECT user_id, count(id) eventCount
                FROM user_performed_skill
                WHERE
                      skill_id = :skillId and
                      project_id = :projectId
                GROUP BY user_id
                ) eventsByUserId
            WHERE
                  eventsByUserId.eventCount >= :numOfOccurrences and
                NOT EXISTS (
                        SELECT id FROM user_achievement WHERE project_id = :projectId and skill_id = :skillId and eventsByUserId.user_id = eventsByUserId.user_id
                    )'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("skillRefId", skillRefId)
        query.setParameter("numOfOccurrences", numOfOccurrences)
        query.executeUpdate()
    }
}
