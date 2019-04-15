/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.ratelimit;

import io.gravitee.repository.mongodb.ratelimit.model.MongoRateLimit;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.Single;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava2Adapter;

import javax.annotation.PostConstruct;
import java.util.function.Supplier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoRateLimitRepository implements RateLimitRepository {

    /*
    @Autowired
    ReactiveMongoOperations operations;
    */

    @Autowired
    @Qualifier("rateLimitMongoTemplate")
    private ReactiveMongoOperations mongoOperations;

    private final static String RATE_LIMIT_COLLECTION = "ratelimit";

    private final static String FIELD_KEY = "_id";
    private final static String FIELD_COUNTER = "counter";
    private final static String FIELD_RESET_TIME = "reset_time";
    private final static String FIELD_LAST_REQUEST = "last_request";
    private final static String FIELD_UPDATED_AT = "updated_at";
    private final static String FIELD_CREATED_AT = "created_at";
    private final static String FIELD_ASYNC = "async";

    private final Update INC_AND_GET_UPDATE = new Update().inc(FIELD_COUNTER, 1);
    private final Query INC_AND_GET_QUERY = new Query(Criteria.where(FIELD_KEY).is("key"));
    private final FindAndModifyOptions INC_AND_GET_OPTIONS = new FindAndModifyOptions().returnNew(true).upsert(false);


    @PostConstruct
    public void ensureTTLIndex() {
        mongoOperations.indexOps(RATE_LIMIT_COLLECTION).ensureIndex(new IndexDefinition() {
            @Override
            public Document getIndexKeys() {
                return new Document(FIELD_RESET_TIME, 1L);
            }

            @Override
            public Document getIndexOptions() {
                // To expire Documents at a Specific Clock Time we have to specify an expireAfterSeconds value of 0.
                return new Document("expireAfterSeconds", 0L);
            }
        });
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, Supplier<RateLimit> supplier) {
        return RxJava2Adapter.monoToSingle(
                mongoOperations
                        .findAndModify(
                                INC_AND_GET_QUERY,
                                INC_AND_GET_UPDATE,
                                INC_AND_GET_OPTIONS,
                                MongoRateLimit.class)
                        .switchIfEmpty(mongoOperations.insert(new MongoRateLimit(supplier.get())))
                        .map(this::convert));
    }

    /*
    @Override
    public RateLimit get(String rateLimitKey) {
        //because RateLimit has no default constructor, we have to manually map values
        Document result = mongoOperations.findById(rateLimitKey, Document.class, RATE_LIMIT_COLLECTION);
        RateLimit rateLimit = convert(result);
        return rateLimit == null ? new RateLimit(rateLimitKey) : rateLimit;
    }

    @Override
    public void save(RateLimit rateLimit) {
        final DBObject doc = BasicDBObjectBuilder.start()
                .add(FIELD_KEY, rateLimit.getKey())
                .add(FIELD_COUNTER, rateLimit.getCounter())
                .add(FIELD_LAST_REQUEST, rateLimit.getLastRequest())
                .add(FIELD_RESET_TIME, new Date(rateLimit.getResetTime()))
                .add(FIELD_UPDATED_AT, rateLimit.getUpdatedAt())
                .add(FIELD_CREATED_AT, rateLimit.getCreatedAt())
                .add(FIELD_ASYNC, rateLimit.isAsync())
                .get();

        mongoOperations.save(doc, RATE_LIMIT_COLLECTION);
    }

    @Override
    public Iterator<RateLimit> findAsyncAfter(long timestamp) {
        final Query query = Query.query(Criteria.where(FIELD_ASYNC).is(true).and(FIELD_UPDATED_AT).gte(timestamp));
        return mongoOperations.find(query, Document.class, RATE_LIMIT_COLLECTION)
                .stream()
                .map(this::convert)
                .iterator();
    }
    */

    private RateLimit convert(MongoRateLimit mongoRateLimit) {
        if (mongoRateLimit == null) {
            return null;
        }

        RateLimit rateLimit = new RateLimit(mongoRateLimit.getId());
        rateLimit.setCounter(mongoRateLimit.getCounter());
        rateLimit.setResetTime(mongoRateLimit.getResetTime().getTime());
        rateLimit.setAsync(mongoRateLimit.isAsync());

        return rateLimit;
    }
}
