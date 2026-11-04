package ua.edu.kneu.articles;
public record GenerationResponse(SaveArticleToEntity article, long clock,
                                 String model, boolean testMode) {}
