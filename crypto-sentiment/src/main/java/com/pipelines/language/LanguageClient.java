package com.pipelines.language;

import java.util.List;

import com.pipelines.data.Tweet;
import com.pipelines.model.EntitySentiment;

public interface LanguageClient {
    public Tweet translate(Tweet tweet, String targetLanguage);

    public List<EntitySentiment> getEntitySentiment(Tweet tweet);
}
