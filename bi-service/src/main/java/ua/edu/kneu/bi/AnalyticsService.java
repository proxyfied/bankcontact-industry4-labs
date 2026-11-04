package ua.edu.kneu.bi;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AnalyticsService {
 private final ContactRecordRepository repository;
 public AnalyticsService(ContactRecordRepository repository) { this.repository=repository; }
 public record Bucket(String label,long count) {}
 public record Statistics(long total,long totalMinutes,List<Bucket> categories,List<Bucket> statuses,List<Bucket> dates) {}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Statistics statistics() {
  return new Statistics(repository.count(),repository.totalMinutes(),buckets(repository.byCategory()),buckets(repository.byStatus()),buckets(repository.byDate()));
 }
 private List<Bucket> buckets(List<Object[]> rows) {
  return rows.stream().map(r->new Bucket(r[0].toString(),((Number)r[1]).longValue())).toList();
 }
}
