package org.inventivetalent.data.mapper;

import com.google.gson.JsonObject;
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
public abstract class AsyncStringValueMapper {

	public static AsyncDataProvider<String> redis(RedisDataProvider provider) {
		return provider;
	}

	public static AsyncDataProvider<String> sql(SQLDataProvider provider) {
		return provider;
	}

	public static AsyncDataProvider<String> mongoDb(MongoDbDataProvider provider) {
		return new AsyncDataProvider<String>() {

			@Override
			public void execute(Runnable runnable) {
				provider.execute(runnable);
			}

			@Override
			public Executor getExecutor() {
				return provider.getExecutor();
			}

			public JsonObject makeValue(String value) {
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("value", value);
				return jsonObject;
			}

			public String getValue(JsonObject jsonObject) {
				return jsonObject.get("value").getAsString();
			}

			@Override
			public void put(@NonNull String key, @NonNull String value) {
				provider.put(key, makeValue(value));
			}

			@Override
			public void put(@NonNull String key, @NonNull DataCallable<String> valueCallable) {
				provider.put(key, () -> makeValue(valueCallable.provide()));
			}

			@Override
			public void putAll(@NonNull Map<String, String> map) {
				Map<String, JsonObject> jsonMap = new HashMap<>();
				for (Map.Entry<String, String> entry : map.entrySet()) {
					jsonMap.put(entry.getKey(), makeValue(entry.getValue()));
				}
				provider.putAll(jsonMap);
			}

			@Override
			public void putAll(@NonNull DataCallable<Map<String, String>> mapCallable) {
				provider.putAll(() -> {
                    Map<String, String> map = mapCallable.provide();
                    Map<String, JsonObject> jsonMap = new HashMap<>();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        jsonMap.put(entry.getKey(), makeValue(entry.getValue()));
                    }
                    return jsonMap;
                });
			}

			@Override
			public void get(@NonNull String key, @NonNull DataCallback<String> callback) {
				provider.get(key, jsonObject -> callback.provide(getValue(jsonObject)));
			}

			@Override
			public void contains(@NonNull String key, @NonNull DataCallback<Boolean> callback) {
				provider.contains(key, callback);
			}

			@Override
			public void remove(@NonNull String key, @NonNull DataCallback<String> callback) {
				provider.remove(key, jsonObject -> callback.provide(getValue(jsonObject)));
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
			public void entries(@NonNull DataCallback<Map<String, String>> callback) {
				provider.entries(stringJsonObjectMap -> {
					Map<String, String> stringMap = new HashMap<>();
					if (stringJsonObjectMap == null) {
						callback.provide(stringMap);
						return;
					}
					for (Map.Entry<String, JsonObject> entry : stringJsonObjectMap.entrySet()) {
						stringMap.put(entry.getKey(), getValue(entry.getValue()));
					}
					callback.provide(stringMap);
				});
			}

			@Override
			public void size(@NonNull DataCallback<Integer> callback) {
				provider.size(callback);
			}
		};
	}

	public static <B extends KeyValueBean> AsyncDataProvider<String> ebean(EbeanDataProvider<B> provider, BeanProvider<B> beanProvider) {
		return new AsyncDataProvider<String>() {

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
			public void put(@NonNull String key, @NonNull String value) {
				provider.put(key, createBean(key, value));
			}

			@Override
			public void put(@NonNull String key, @NonNull DataCallable<String> valueCallable) {
				provider.put(key, () -> createBean(key, valueCallable.provide()));
			}

			@Override
			public void putAll(@NonNull Map<String, String> map) {
				Map<String, B> beanMap = new HashMap<>();
				for (Map.Entry<String, String> entry : map.entrySet()) {
					beanMap.put(entry.getKey(), createBean(entry.getKey(), entry.getValue()));
				}
				provider.putAll(beanMap);
			}

			@Override
			public void putAll(@NonNull DataCallable<Map<String, String>> mapCallable) {
				provider.putAll(() -> {
                    Map<String, String> map = mapCallable.provide();
                    Map<String, B> beanMap = new HashMap<>();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        beanMap.put(entry.getKey(), createBean(entry.getKey(), entry.getValue()));
                    }
                    return beanMap;
                });
			}

			@Override
			public void get(@NonNull String key, @NonNull DataCallback<String> callback) {
				provider.get(key, keyValueBean -> callback.provide(keyValueBean != null ? keyValueBean.getValue() : null));
			}

			@Override
			public void contains(@NonNull String key, @NonNull DataCallback<Boolean> callback) {
				provider.contains(key, callback);
			}

			@Override
			public void remove(@NonNull String key, @NonNull DataCallback<String> callback) {
				provider.remove(key, keyValueBean -> callback.provide(keyValueBean != null ? keyValueBean.getValue() : null));
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
			public void entries(@NonNull DataCallback<Map<String, String>> callback) {
				provider.entries(stringBMap -> {
					Map<String, String> stringMap = new HashMap<>();
					if (stringBMap == null) {
						callback.provide(null);
						return;
					}
					for (Map.Entry<String, B> entry : stringBMap.entrySet()) {
						stringMap.put(entry.getKey(), entry.getValue().getValue());
					}
					callback.provide(stringMap);
				});
			}

			@Override
			public void size(@NonNull DataCallback<Integer> callback) {
				provider.size(callback);
			}
		};
	}

}
