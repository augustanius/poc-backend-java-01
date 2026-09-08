ALTER TABLE poc_dlq.dlq_messages
ADD COLUMN original_topic VARCHAR(200);

UPDATE poc_dlq.dlq_messages
SET original_topic = REPLACE(topic, '.dlq.', '.')
WHERE original_topic IS NULL;

CREATE INDEX idx_dlq_messages_original_topic ON poc_dlq.dlq_messages(original_topic);
