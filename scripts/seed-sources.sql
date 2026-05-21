INSERT INTO oracle_sources (name, type, base_url, enabled, priority)
VALUES
  ('coingecko-demo', 'HTTP_API', 'https://api.example.com/outcome', true, 10),
  ('official-feed', 'HTTP_API', 'https://feed.example.com/v1/outcome', true, 20)
ON CONFLICT (name) DO NOTHING;
