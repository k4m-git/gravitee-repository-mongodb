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
package io.gravitee.repository.mongodb.ratelimit.model;

import io.gravitee.repository.ratelimit.model.RateLimit;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document("ratelimit")
public class MongoRateLimit {

    @Id
    private String id;

    private Date resetTime;

    private long counter;

    private long limit;

    private boolean async;

    public MongoRateLimit(final RateLimit rateLimit) {
        this.id = rateLimit.getKey();
        this.resetTime = new Date(rateLimit.getResetTime());
        this.counter = rateLimit.getCounter();
        this.async = rateLimit.isAsync();
        this.limit = -1;
    }

    public MongoRateLimit() {
        // Default constructor
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getResetTime() {
        return resetTime;
    }

    public void setResetTime(Date resetTime) {
        this.resetTime = resetTime;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
