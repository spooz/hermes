package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.ClassRule;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.frontend.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TypedIndex.PUBLISHED_MESSAGES;

public class ElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";

    @ClassRule
    public static ElasticsearchResource elasticsearch = new ElasticsearchResource(PUBLISHED_MESSAGES);

    @Override
    protected LogRepository createRepository() {
        return new ElasticsearchLogRepository(elasticsearch.client(), CLUSTER_NAME, 1000, 100, new MetricRegistry(), new PathsCompiler("localhost"));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status, String reason) throws Exception {
        awaitUntilMessageIsIndexed(
                boolQuery()
                        .should(matchQuery(TOPIC_NAME, topic))
                        .should(matchQuery(MESSAGE_ID, id))
                        .should(matchQuery(STATUS, status.toString()))
                        .should(matchQuery(REASON, reason))
                        .should(matchQuery(CLUSTER, CLUSTER_NAME)));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status) throws Exception {
        awaitUntilMessageIsIndexed(
                boolQuery()
                        .should(matchQuery(TOPIC_NAME, topic))
                        .should(matchQuery(MESSAGE_ID, id))
                        .should(matchQuery(STATUS, status.toString()))
                        .should(matchQuery(CLUSTER, CLUSTER_NAME)));
    }

    private void awaitUntilMessageIsIndexed(QueryBuilder query) {
        await().atMost(ONE_MINUTE).until(() -> {
            SearchResponse response = elasticsearch.client().prepareSearch(PUBLISHED_MESSAGES.getIndex())
                    .setTypes(PUBLISHED_MESSAGES.getType())
                    .setQuery(query)
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }

}