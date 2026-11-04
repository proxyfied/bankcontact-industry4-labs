package ua.edu.kneu.bi;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ContactRecordRepository extends JpaRepository<ContactRecord,Long> {
 @Query("select r.externalCode from ContactRecord r where r.externalCode in :codes")
 List<String> existingCodes(Collection<String> codes);
 @Query("select r.category, count(r) from ContactRecord r group by r.category order by r.category")
 List<Object[]> byCategory();
 @Query("select r.status, count(r) from ContactRecord r group by r.status order by r.status")
 List<Object[]> byStatus();
 @Query("select r.registeredOn, count(r) from ContactRecord r group by r.registeredOn order by r.registeredOn")
 List<Object[]> byDate();
 @Query("select coalesce(sum(r.handlingMinutes),0) from ContactRecord r")
 long totalMinutes();
}
