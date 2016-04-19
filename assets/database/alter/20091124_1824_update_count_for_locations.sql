REPLACE INTO locations(_id,name,datetime,provider,accuracy,latitude,longitude,is_payee,resolved_address,count) 
select l._id,l.name,l.datetime,l.provider,l.accuracy,l.latitude,l.longitude,l.is_payee,l.resolved_address,count(*) 
from transactions t inner join locations l where t.location_id=l._id group by l._id;