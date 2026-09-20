package gr.uoa.di.networkanalysis;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class DateDifferenceTest {

	public void daysBetweenDates() {
		LocalDate d1 = LocalDate.of(2003, 10, 5);
		LocalDate d2 = LocalDate.of(2004, 10, 5);
		Instant one = Instant.ofEpochSecond(1845836728);
	    Instant two = Instant.ofEpochSecond(1846866935);
	    Duration res = Duration.between(one, two);
	    System.out.println(res.toDays());
		//Duration d = Duration.between(d1, d2);
	    //long diff = Math.abs(duration.toDays());
		//System.out.println(diff);
	}
	
	public void daysBetweenDatesAfterEpochToLocalDateConversion() {
		
		long timeInSeconds1 = 1162422000;
		LocalDateTime ldt1 = LocalDateTime.ofEpochSecond(timeInSeconds1, 0, ZoneOffset.UTC);
		System.out.println(ldt1.getYear()+ " " + ldt1.getMonthValue() + " " + ldt1.getDayOfMonth());
		
		long timeInSeconds2 = 1178748000;
		LocalDateTime ldt2 = LocalDateTime.ofEpochSecond(timeInSeconds2, 0, ZoneOffset.UTC);
		System.out.println(ldt2.getYear()+ " " + ldt2.getMonthValue() + " " + ldt2.getDayOfMonth());
		
		
		LocalDate d1 = LocalDate.of(ldt1.getYear(), ldt1.getMonthValue(), ldt1.getDayOfMonth());
		LocalDate d2 = LocalDate.of(ldt2.getYear(), ldt2.getMonthValue(), ldt2.getDayOfMonth());
		long daysBetween = ChronoUnit.DAYS.between(d1, d2);
		System.out.println(daysBetween);
	}
	
	public void sanityCheck() {
		Instant one = Instant.ofEpochSecond(1178748000);
		Instant two = Instant.ofEpochSecond(1178665200);
		Duration res = Duration.between(one, two);
	    //System.out.println(res.toSeconds());
	}
}
