UPDATE budget SET start_date=0 where start_date is null;

UPDATE budget SET end_date=strftime('%s',datetime('now','start of year','+10 years','-1 second'))||'999' where end_date is null;

UPDATE budget SET recur = 'NO_RECUR,startDate='||start_date||',period=STOPS_ON_DATE,periodParam='||end_date;