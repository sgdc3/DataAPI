package org.inventivetalent.data.mapper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import org.inventivetalent.data.async.AsyncDataProvider;
import org.inventivetalent.data.async.DataCallable;
import org.inventivetalent.data.async.DataCallback;
import org.inventivetalent.data.ebean.BeanProvider;
import org.inventivetalent.data.ebean.EbeanDataProvider;
import org.inventivetalent.data.ebean.KeyValueBean;
import org.inventivetalent.data.mongodb.MongoDbDataProvider;
import org.inventivetalent.data.redis.RedisDataProvider;
import org.inventivetalent.data.sql.SQLDataProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@SuppressWarnings({"unused", "WeakerAccess"})
public class AsyncJsonValueMapper {

	public static AsyncDataProvider<JsonObject> mongodb(MongoDbDataProvider provider) {
		return provider;
	}

	public static AsyncDataProvider<JsonObject> redis(RedisDataProvider provider) {
		return new StringToJsonMapper(provider);
	}

	public static AsyncDataProvider<JsonObject> sql(SQLDataProvider provider) {
		return new StringToJsonMapper(provider);
	}

	public static <B extends KeyValueBean> AsyncDataProvider<JsonObject> ebean(EbeanDataProvider<B> provider, BeanProvider<B> beanProvider) {
		return new AsyncDataProvider<JsonObject>() {

			final JsonParser parser = new JsonParser();

			@Override
			public void execute(Runnable runnable) {
				provider.execute(runnable);
			}

			@Override
			public Executor getExecutor() {
				return provider.getExecutor();
			}

			B createBean(String key, String value) {
				return beanProvider.provide(key, value);
			}

			@Override
			public void put(@NonNull String key, @NonNull JsonObject value) {
				provider.put(key, createBean(key, value.toString()));
			}

			@Override
			public void put(@NonNull String key, @NonNull DataCallable<JsonObject> valueCallable) {
				provider.put(key, () -> createBean(key, valueCallable.provide().toString()));
			}

			@Override
			public void putAll(@NonNull Map<String, JsonObject> map) {
				Map<String, B> beanMap = new HashMap<>();
				for (Map.Entry<String, JsonObject> entry : map.entrySet()) {
					beanMap.put(entry.getKey(), createBean(entry.getKey(), entry.getValue().toString()));
				}
				provider.putAll(beanMap);
			}

			@Override
			public void putAll(@NonNull DataCallable<Map<String, JsonObject>> mapCallable) {
				provider.putAll(() -> {
                    Map<String, JsonObject> jsonMap = mapCallable.provide();
                    Map<String, B> beanMap = new HashMap<>();
                    for (Map.Entry<String, JsonObject> entry : jsonMap.entrySet()) {
                        beanMap.put(entry.getKey(), createBean(entry.getKey(), entry.getValue().toString()));
                    }
                    return beanMap;
                });
			}

			@Override
			public void get(@NonNull String key, @NonNull DataCallback<JsonObject> callback) {
				provider.get(key, keyValueBean -> callback.provide(keyValueBean != null ? parser.parse(keyValueBean.getValue()).getAsJsonObject() : null));
			}

			@Override
			public void contains(@NonNull String key, @NonNull DataCallback<Boolean> callback) {
				provider.contains(key, callback);
			}

			@Override
			public void remove(@NonNull String key, @NonNull DataCallback<JsonObject> callback) {
				provider.remove(key, keyValueBean -> callback.provide(keyValueBean != null ? parser.parse(keyValueBean.getValue()).getAsJsonObject() : null));
			}

			@Override
			public void remove(@NonNull String key) {
				provider.remove(key);
			}

			@Override
			public void keys(@NonNull DataCallback<Collection<String>> callback) {
				provider.keys(callback);
			}

			@Override
			public void entries(@NonNull DataCallback<Map<String, JsonObject>> callback) {
				provider.entries(stringBMap -> {
					Map<String, JsonObject> jsonMap = new HashMap<>();
					if (stringBMap == null) {
						callback.provide(jsonMap);
						return;
					}
					for (Map.Entry<String, B> entry : stringBMap.entrySet()) {
						jsonMap.put(entry.getKey(), parser.parse(entry.getValue().getValue()).getAsJsonObject());
					}
					callback.provide(jsonMap);
				});
			}

			@Override
			public void size(@NonNull DataCallback<Integer> callback) {
				provider.size(callback);
			}
		};
	}

	static class StringToJsonMapper implements AsyncDataProvider<JsonObject> {

		final JsonParser parser = new JsonParser();
		private AsyncDataProvider<String> provider;

		public StringToJsonMapper(AsyncDataProvider<String> provider) {
			this.provider = provider;
		}

		@Override
		public void execute(Runnable runnable) {
			provider.execute(runnable);
		}

		@Override
		public Executor getExecutor() {
			return provider.getExecutor();
		}

		@Override
		public void put(@NonNull String key, @NonNull JsonObject value) {
			provider.put(key, value.toString());
		}

		@Override
		public void put(@NonNull String key, @NonNull DataCallable<JsonObject> valueCallable) {
			provider.put(key, () -> valueCallable.provide().toString());
		}

		@Override
		public void putAll(@NonNull Map<String, JsonObject> map) {
			Map<String, String> stringMap = new HashMap<>();
			for (Map.Entry<String, JsonObject> entry : map.entrySet()) {
				stringMap.put(entry.getKey(), entry.getValue().toString());
			}
			provider.putAll(stringMap);
		}

		@Override
		public void putAll(@NonNull DataCallable<Map<String, JsonObject>> mapCallable) {
			provider.putAll(() -> {
                Map<String, JsonObject> jsonMap = mapCallable.provide();
                Map<String, String> stringMap = new HashMap<>();
                for (Map.Entry<String, JsonObject> entry : jsonMap.entrySet()) {
                    stringMap.put(entry.getKey(), entry.getValue().toString());
                }
                return stringMap;
            });
		}

		@Override
		public void get(@NonNull String key, @NonNull DataCallback<JsonObject> callback) {
			provider.get(key, s -> callback.provide(parser.parse(s).getAsJsonObject()));
		}

		@Override
		public void contains(@NonNull String key, @NonNull DataCallback<Boolean> callback) {
			provider.contains(key, callback);
		}

		@Override
		public void remove(@NonNull String key, @NonNull DataCallback<JsonObject> callback) {
			provider.remove(key, s -> callback.provide(parser.parse(s).getAsJsonObject()));
		}

		@Override
		public void remove(@NonNull String key) {
			provider.remove(key);
		}

		@Override
		public void keys(@NonNull DataCallback<Collection<String>> callback) {
			provider.keys(callback);
		}

		@Override
		public void entries(@NonNull DataCallback<Map<String, JsonObject>> callback) {
			provider.entries(map -> {
				Map<String, JsonObject> jsonMap = new HashMap<>();
				if (map == null) {
					callback.provide(jsonMap);
					return;
				}
				for (Map.Entry<String, String> entry : map.entrySet()) {
					jsonMap.put(entry.getKey(), parser.parse(entry.getValue()).getAsJsonObject());
				}
				callback.provide(jsonMap);
			});
		}

		@Override
		public void size(@NonNull DataCallback<Integer> callback) {
			provider.size(callback);
		}
	}

}
