import { z } from 'zod';
import { config as dotenvConfig } from 'dotenv';

dotenvConfig();

const serverEnvSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test', 'preview']).optional(),
  API_URL: z.string().url().optional().default('http://localhost:8080'),
});

const serverConfig = serverEnvSchema.parse({
  NODE_ENV: process.env.NODE_ENV,
  API_URL: process.env.API_URL,
});

export default serverConfig;
