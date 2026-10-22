package com.mihrab.tv.data;

import java.time.Instant;

/** Replace the local snapshot provider in the next data phase; never imply live prayer accuracy. */
public interface HomeRepository {
  HomeSnapshot load(Instant instant) throws Exception;
}
