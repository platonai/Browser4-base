/**
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file defines the types and helper functions for declaring and parsing CLI commands.
 * It includes schemas for arguments and options, and functions to parse raw arguments into
 * structured data using Zod validation.
 */

import { z } from '../../mcpBundle';

import type zodType from 'zod';

export type Category = 'core' | 'navigation' | 'keyboard' | 'mouse' | 'export' | 'storage' | 'tabs' | 'network' | 'devtools' | 'browsers' | 'config' | 'install';

export type CommandSchema<Args extends zodType.ZodTypeAny, Options extends zodType.ZodTypeAny> = {
  name: string;
  category: Category;
  description: string;
  hidden?: boolean;
  args?: Args;
  options?: Options;
  toolName: string | ((args: zodType.infer<Args> & zodType.infer<Options>) => string);
  toolParams: (args: zodType.infer<Args> & zodType.infer<Options>) => any;
};

export type AnyCommandSchema = CommandSchema<any, any>;

export function declareCommand<Args extends zodType.ZodTypeAny, Options extends zodType.ZodTypeAny>(command: CommandSchema<Args, Options>): CommandSchema<Args, Options> {
  // Returns the command schema as is, providing type inference for arguments and options.
  return command;
}

const kEmptyOptions = z.object({});
const kEmptyArgs = z.object({});

export function parseCommand(command: AnyCommandSchema, args: Record<string, string> & { _: string[] }): { toolName: string, toolParams: any } {
  // Extract options from the arguments object, excluding the positional arguments array.
  const optionsObject = { ...args } as Record<string, string>;
  delete optionsObject['_'];
  const optionsSchema = (command.options ?? kEmptyOptions).strict();
  // Validate and parse the options against the schema.
  const options: Record<string, string> = zodParse(optionsSchema, optionsObject, 'option');

  const argsSchema = (command.args ?? kEmptyArgs).strict();
  const argNames = [...Object.keys(argsSchema.shape)];
  // Positional arguments start from index 1 (index 0 is the command name itself).
  const argv = args['_'].slice(1);
  if (argv.length > argNames.length)
    throw new Error(`error: too many arguments: expected ${argNames.length}, received ${argv.length}`);
  const argsObject: Record<string, string> = {};
  // Map positional arguments to their names defined in the schema.
  argNames.forEach((name, index) => argsObject[name] = argv[index]);
  // Validate and parse the positional arguments against the schema.
  const parsedArgsObject: Record<string, string> = zodParse(argsSchema, argsObject, 'argument');

  // Determine the tool name and parameters based on the parsed arguments and options.
  const toolName = typeof command.toolName === 'function' ? command.toolName({ ...parsedArgsObject, ...options }) : command.toolName;
  const toolParams = command.toolParams({ ...parsedArgsObject, ...options });
  return { toolName, toolParams };
}

function zodParse(schema: zodType.ZodAny, data: unknown, type: 'option' | 'argument'): any {
  // Validate data against the Zod schema and handle errors gracefully.
  try {
    return schema.parse(data);
  } catch (e) {
    throw new Error((e as zodType.ZodError).issues.map(issue => {
      const keys: string[] = (issue as any).keys || [''];
      const props = keys.map(key => [...issue.path, key].filter(Boolean).join('.'));
      return props.map(prop => {
        const label = type === 'option' ? `'--${prop}' option` : `'${prop}' argument`;
        switch (issue.code) {
          case 'invalid_type':
            return 'error: ' + label + ': ' + issue.message.replace(/Invalid input:/, '').trim();
          case 'unrecognized_keys':
            return 'error: unknown ' + label;
          default:
            return 'error: ' + label + ': ' + issue.message;
        }
      });
    }).flat().join('\n'));
  }
}
