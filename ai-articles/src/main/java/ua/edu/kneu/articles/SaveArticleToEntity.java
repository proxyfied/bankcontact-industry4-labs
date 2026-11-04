package ua.edu.kneu.articles;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveArticleToEntity(
    String title_of_the_article,
    String author_set,
    String year_of_publication,
    String name_journal,
    String volume,
    String issue,
    String first_page,
    String last_page,
    String total_volume_of_printed_sheets,
    String doi,
    String issn,
    String eissn,
    String isbn,
    String url,
    String publisher,
    String publication_city,
    String publication_country,
    String language,
    String published_in_ukraine,
    String published_abroad,
    String specialized,
    String publication_type,
    String abstract_text,
    String keywords,
    String affiliation,
    String author_orcid,
    String udc,
    String references_count,
    String indexed_scopus,
    String indexed_web_of_science,
    String peer_reviewed,
    String open_access,
    String publication_date,
    String citation_text,
    String notes,
    String source_text) {
    public SaveArticleToEntity {
        title_of_the_article = value(title_of_the_article, "");
        author_set = value(author_set, "");
        year_of_publication = value(year_of_publication, "0");
        name_journal = value(name_journal, "");
        volume = value(volume, "0");
        issue = value(issue, "0");
        first_page = value(first_page, "0");
        last_page = value(last_page, "0");
        total_volume_of_printed_sheets = value(total_volume_of_printed_sheets, "0");
        doi = value(doi, "");
        issn = value(issn, "");
        eissn = value(eissn, "");
        isbn = value(isbn, "");
        url = value(url, "");
        publisher = value(publisher, "");
        publication_city = value(publication_city, "");
        publication_country = value(publication_country, "");
        language = value(language, "");
        published_in_ukraine = value(published_in_ukraine, "0");
        published_abroad = value(published_abroad, "0");
        specialized = value(specialized, "");
        publication_type = value(publication_type, "");
        abstract_text = value(abstract_text, "");
        keywords = value(keywords, "");
        affiliation = value(affiliation, "");
        author_orcid = value(author_orcid, "");
        udc = value(udc, "");
        references_count = value(references_count, "0");
        indexed_scopus = value(indexed_scopus, "0");
        indexed_web_of_science = value(indexed_web_of_science, "0");
        peer_reviewed = value(peer_reviewed, "0");
        open_access = value(open_access, "0");
        publication_date = value(publication_date, "");
        citation_text = value(citation_text, "");
        notes = value(notes, "");
        source_text = value(source_text, "");
    }
    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
