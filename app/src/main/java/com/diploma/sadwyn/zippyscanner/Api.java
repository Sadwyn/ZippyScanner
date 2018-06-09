package com.diploma.sadwyn.zippyscanner;

import com.diploma.sadwyn.zippyscanner.pojo.TranslationResult;

import io.reactivex.Single;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface Api {
    @POST("/language/translate/v2")
    Single<TranslationResult> translate(@Query("q") String text,
                                        @Query("target") String target,
                                        @Query("format") String format,
                                        @Query("source") String source,
                                        @Query("model") String model,
                                        @Query("key") String key);

}
