DELETE FROM FRAGMENT;
-- clear all via dataspace table cleanup
-- all other data will be removed by cascade
DELETE FROM DATASPACE;
-- explicit clear
DELETE FROM YANG_RESOURCE;
