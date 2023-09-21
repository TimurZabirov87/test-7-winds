package mobi.sevenwinds.app

import org.joda.time.DateTime
import java.time.LocalDateTime
import java.time.ZoneOffset


fun DateTime.toLDT(): LocalDateTime {
    return LocalDateTime.ofEpochSecond(this.millis, 0, ZoneOffset.UTC)
}
