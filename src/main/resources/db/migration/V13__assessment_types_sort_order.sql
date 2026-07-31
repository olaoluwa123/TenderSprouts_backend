ALTER TABLE assessment_types ADD COLUMN sort_order INT DEFAULT 0;

UPDATE assessment_types SET sort_order = 1 WHERE code = 'CA';
UPDATE assessment_types SET sort_order = 2 WHERE code = 'PROJECT';
UPDATE assessment_types SET sort_order = 3 WHERE code = 'EXAM';
