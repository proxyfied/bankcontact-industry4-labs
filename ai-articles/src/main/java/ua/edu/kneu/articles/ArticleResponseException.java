package ua.edu.kneu.articles;
public class ArticleResponseException extends RuntimeException {
    public ArticleResponseException() { super("AI повернув некоректну або суперечливу структуру JSON. Спробуйте уточнити цитування."); }
}
