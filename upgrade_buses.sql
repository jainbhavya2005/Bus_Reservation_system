-- ============================================================
-- Bus Reservation System: bus types + weekly schedules + new fleet
-- Run once in MySQL Workbench / mysql client on the `tickets` database.
-- ============================================================
USE tickets;

-- 1) New columns (run this part only once; it errors if the columns already exist)
ALTER TABLE bus
  ADD COLUMN bus_type VARCHAR(30) NOT NULL DEFAULT 'Ordinary',
  ADD COLUMN run_days VARCHAR(40) NOT NULL DEFAULT 'DAILY';
-- run_days is either DAILY or a comma list without spaces, e.g. MON,WED,FRI

-- 2) Remove the old test buses and bookings (skip these 4 lines to keep your old data)
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM booking;
DELETE FROM bus;
ALTER TABLE booking AUTO_INCREMENT = 1;
ALTER TABLE bus AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- 3) New fleet: variety of routes, bus types, prices and weekly schedules
INSERT INTO bus (bus_name, source, destination, departure_time, journey_hours, total_seats, booked_seats, price, bus_type, run_days) VALUES
-- Patiala -> Delhi
('PRTC Ordinary 101',        'Patiala',    'Delhi',      '05:30:00', 6, 52, 0,  260, 'Ordinary',     'DAILY'),
('PUNBUS Express 102',       'Patiala',    'Delhi',      '07:15:00', 5, 49, 0,  340, 'Express',      'DAILY'),
('PRTC Volvo AC 103',        'Patiala',    'Delhi',      '06:00:00', 5, 45, 0,  650, 'Volvo AC',     'DAILY'),
('PUNBUS Super Luxury 104',  'Patiala',    'Delhi',      '14:15:00', 5, 42, 0,  520, 'Super Luxury', 'TUE,THU,SAT'),
('Orbit Semi-Sleeper 105',   'Patiala',    'Delhi',      '22:30:00', 6, 36, 0,  780, 'Semi-Sleeper', 'MON,WED,FRI'),
('Delhi Night Sleeper 106',  'Patiala',    'Delhi',      '23:30:00', 7, 30, 0,  900, 'AC Sleeper',   'FRI,SUN'),
-- Patiala -> Chandigarh
('PRTC Ordinary 201',        'Patiala',    'Chandigarh', '06:30:00', 2, 50, 0,  120, 'Ordinary',     'DAILY'),
('PUNBUS Deluxe 202',        'Patiala',    'Chandigarh', '09:00:00', 2, 40, 0,  180, 'Deluxe',       'DAILY'),
('Commuter Express 203',     'Patiala',    'Chandigarh', '17:30:00', 2, 45, 0,  150, 'Express',      'MON,TUE,WED,THU,FRI'),
-- Patiala -> Amritsar
('PRTC AC Seater 301',       'Patiala',    'Amritsar',   '09:00:00', 4, 40, 0,  420, 'AC Seater',    'DAILY'),
('Golden Temple Express 302','Patiala',    'Amritsar',   '21:00:00', 5, 36, 0,  560, 'Semi-Sleeper', 'TUE,SAT'),
-- Patiala -> Ludhiana
('PRTC Local 401',           'Patiala',    'Ludhiana',   '07:45:00', 2, 50, 0,  140, 'Ordinary',     'DAILY'),
('Ludhiana Intercity 402',   'Patiala',    'Ludhiana',   '16:00:00', 2, 45, 0,  170, 'Express',      'MON,WED,FRI'),
-- Patiala -> hills and Rajasthan
('HRTC Himdhara 501',        'Patiala',    'Shimla',     '06:00:00', 7, 40, 0,  520, 'Deluxe',       'DAILY'),
('Shimla Volvo Weekend 502', 'Patiala',    'Shimla',     '20:30:00', 8, 36, 0,  890, 'Volvo AC',     'FRI,SAT'),
('Manali Sleeper 503',       'Patiala',    'Manali',     '18:30:00', 11, 30, 0, 1450,'AC Sleeper',   'THU,SAT'),
('Doon Express 504',         'Patiala',    'Dehradun',   '05:45:00', 8, 45, 0,  560, 'Express',      'TUE,FRI'),
('Pink City Express 505',    'Patiala',    'Jaipur',     '19:00:00', 9, 40, 0,  780, 'Semi-Sleeper', 'MON,THU'),
-- Return and other routes
('PRTC Volvo AC 601',        'Delhi',      'Patiala',    '17:00:00', 5, 45, 0,  650, 'Volvo AC',     'DAILY'),
('Delhi-Patiala Express 602','Delhi',      'Patiala',    '18:30:00', 5, 49, 0,  340, 'Express',      'DAILY'),
('Return Night Sleeper 603', 'Delhi',      'Patiala',    '23:00:00', 7, 30, 0,  900, 'AC Sleeper',   'SAT,MON'),
('Chandigarh Volvo 701',     'Chandigarh', 'Delhi',      '06:30:00', 5, 45, 0,  700, 'Volvo AC',     'DAILY'),
('PRTC Ordinary Return 702', 'Chandigarh', 'Patiala',    '08:00:00', 2, 50, 0,  120, 'Ordinary',     'DAILY'),
('Amritsar Night Express 801','Amritsar',  'Delhi',      '21:00:00', 9, 42, 0,  850, 'Express',      'DAILY'),
('Amritsar Sleeper 802',     'Amritsar',   'Delhi',      '22:30:00', 9, 30, 0, 1100, 'AC Sleeper',   'MON,WED,FRI'),
('Jalandhar Express 803',    'Jalandhar',  'Delhi',      '06:15:00', 8, 45, 0,  620, 'Express',      'DAILY'),
('Ludhiana Delhi AC 804',    'Ludhiana',   'Delhi',      '07:00:00', 6, 45, 0,  610, 'AC Seater',    'TUE,THU,SAT'),
('HRTC Himdhara Return 901', 'Shimla',     'Patiala',    '08:00:00', 7, 40, 0,  520, 'Deluxe',       'DAILY'),
('Shimla Volvo Return 902',  'Shimla',     'Patiala',    '17:00:00', 8, 36, 0,  890, 'Volvo AC',     'SUN,MON');

-- 4) Check
SELECT bus_id, bus_name, source, destination, bus_type, run_days FROM bus ORDER BY bus_id;
