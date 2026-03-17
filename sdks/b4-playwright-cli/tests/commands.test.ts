/**
 * Tests for CLI command parsing — verifies commands.ts definitions
 * parse correctly through parseCommand.
 */

import {commands} from '../src/cli/daemon/commands';
import {parseCommand} from '../src/cli/daemon/command';

// Helper to build args in the format parseCommand expects.
// The type definition in command.ts is `Record<string, string> & { _: string[] }`
// which is contradictory (string[] vs string), so we cast at the boundary.
function args(positional: string[], options: Record<string, string> = {}): Record<string, string> & { _: string[] } {
    return {...options, _: positional} as any;
}

describe('commands definitions', () => {
    it('should have all expected core commands', () => {
        const expectedCore = [
            'open', 'close', 'goto', 'type', 'click', 'dblclick',
            'fill', 'drag', 'hover', 'select', 'upload', 'check',
            'uncheck', 'snapshot', 'eval', 'dialog-accept', 'dialog-dismiss',
            'resize',
        ];
        for (const name of expectedCore) {
            expect(commands[name]).toBeDefined();
        }
    });

    it('should have navigation commands', () => {
        expect(commands['go-back']).toBeDefined();
        expect(commands['go-forward']).toBeDefined();
        expect(commands['reload']).toBeDefined();
    });

    it('should have keyboard commands', () => {
        expect(commands['press']).toBeDefined();
        expect(commands['keydown']).toBeDefined();
        expect(commands['keyup']).toBeDefined();
    });

    it('should have mouse commands', () => {
        expect(commands['mousemove']).toBeDefined();
        expect(commands['mousedown']).toBeDefined();
        expect(commands['mouseup']).toBeDefined();
        expect(commands['mousewheel']).toBeDefined();
    });

    it('should have tab commands', () => {
        expect(commands['tab-list']).toBeDefined();
        expect(commands['tab-new']).toBeDefined();
        expect(commands['tab-close']).toBeDefined();
        expect(commands['tab-select']).toBeDefined();
    });

    it('should have export commands', () => {
        expect(commands['screenshot']).toBeDefined();
        expect(commands['pdf']).toBeDefined();
    });

    it('should have session management commands', () => {
        expect(commands['list']).toBeDefined();
        expect(commands['close-all']).toBeDefined();
        expect(commands['kill-all']).toBeDefined();
    });
});

describe('parseCommand', () => {
    it('should parse goto command', () => {
        const cmd = commands['goto'];
        const {toolName, toolParams} = parseCommand(cmd, args(['goto', 'https://example.com']));
        expect(toolName).toBe('browser_navigate');
        expect(toolParams).toEqual({url: 'https://example.com'});
    });

    it('should parse click command', () => {
        const cmd = commands['click'];
        const {toolName, toolParams} = parseCommand(cmd, args(['click', 'e15']));
        expect(toolName).toBe('browser_click');
        expect(toolParams.ref).toBe('e15');
    });

    it('should parse click command with optional button', () => {
        const cmd = commands['click'];
        const {toolName, toolParams} = parseCommand(cmd, args(['click', 'e15', 'right']));
        expect(toolName).toBe('browser_click');
        expect(toolParams.ref).toBe('e15');
        expect(toolParams.button).toBe('right');
    });

    it('should parse dblclick command', () => {
        const cmd = commands['dblclick'];
        const {toolName, toolParams} = parseCommand(cmd, args(['dblclick', 'e7']));
        expect(toolName).toBe('browser_click');
        expect(toolParams.ref).toBe('e7');
        expect(toolParams.doubleClick).toBe(true);
    });

    it('should parse fill command', () => {
        const cmd = commands['fill'];
        const {toolName, toolParams} = parseCommand(cmd, args(['fill', 'e5', 'user@example.com']));
        expect(toolName).toBe('browser_type');
        expect(toolParams.ref).toBe('e5');
        expect(toolParams.text).toBe('user@example.com');
    });

    it('should parse type command', () => {
        const cmd = commands['type'];
        const {toolName, toolParams} = parseCommand(cmd, args(['type', '#message', 'hello world']));
        expect(toolName).toBe('browser_press_sequentially');
        expect(toolParams.ref).toBe('#message');
        expect(toolParams.text).toBe('hello world');
    });

    it('should parse press command', () => {
        const cmd = commands['press'];
        const {toolName, toolParams} = parseCommand(cmd, args(['press', '#message', 'Enter']));
        expect(toolName).toBe('browser_press_key');
        expect(toolParams.ref).toBe('#message');
        expect(toolParams.key).toBe('Enter');
    });

    it('should parse hover command', () => {
        const cmd = commands['hover'];
        const {toolName, toolParams} = parseCommand(cmd, args(['hover', 'e4']));
        expect(toolName).toBe('browser_hover');
        expect(toolParams.ref).toBe('e4');
    });

    it('should parse select command', () => {
        const cmd = commands['select'];
        const {toolName, toolParams} = parseCommand(cmd, args(['select', 'e9', 'option-value']));
        expect(toolName).toBe('browser_select_option');
        expect(toolParams.ref).toBe('e9');
        expect(toolParams.values).toEqual(['option-value']);
    });

    it('should parse drag command', () => {
        const cmd = commands['drag'];
        const {toolName, toolParams} = parseCommand(cmd, args(['drag', 'e2', 'e8']));
        expect(toolName).toBe('browser_drag');
        expect(toolParams.startRef).toBe('e2');
        expect(toolParams.endRef).toBe('e8');
    });

    it('should parse resize command with numeric args', () => {
        const cmd = commands['resize'];
        const {toolName, toolParams} = parseCommand(cmd, args(['resize', '1920', '1080']));
        expect(toolName).toBe('browser_resize');
        expect(toolParams.width).toBe(1920);
        expect(toolParams.height).toBe(1080);
    });

    it('should parse mousemove command', () => {
        const cmd = commands['mousemove'];
        const {toolName, toolParams} = parseCommand(cmd, args(['mousemove', '150', '300']));
        expect(toolName).toBe('browser_mouse_move_xy');
        expect(toolParams.x).toBe(150);
        expect(toolParams.y).toBe(300);
    });

    it('should parse mousewheel command', () => {
        const cmd = commands['mousewheel'];
        const {toolName, toolParams} = parseCommand(cmd, args(['mousewheel', '0', '100']));
        expect(toolName).toBe('browser_mouse_wheel');
    });

    it('should parse tab-select command', () => {
        const cmd = commands['tab-select'];
        const {toolName, toolParams} = parseCommand(cmd, args(['tab-select', '0']));
        expect(toolName).toBe('browser_tabs');
        expect(toolParams.action).toBe('select');
        expect(toolParams.index).toBe(0);
    });

    it('should parse tab-new with optional URL', () => {
        const cmd = commands['tab-new'];
        const {toolName, toolParams} = parseCommand(cmd, args(['tab-new', 'https://example.com/page']));
        expect(toolName).toBe('browser_tabs');
        expect(toolParams.action).toBe('new');
        expect(toolParams.url).toBe('https://example.com/page');
    });

    it('should parse tab-new without URL', () => {
        const cmd = commands['tab-new'];
        const {toolName, toolParams} = parseCommand(cmd, args(['tab-new']));
        expect(toolName).toBe('browser_tabs');
        expect(toolParams.action).toBe('new');
    });

    it('should parse open with URL', () => {
        const cmd = commands['open'];
        const {toolName, toolParams} = parseCommand(cmd, args(['open', 'https://example.com']));
        expect(toolName).toBe('browser_navigate');
        expect(toolParams.url).toBe('https://example.com');
    });

    it('should parse open without URL', () => {
        const cmd = commands['open'];
        const {toolName, toolParams} = parseCommand(cmd, args(['open']));
        expect(toolName).toBe('browser_snapshot');
        expect(toolParams.url).toBe('about:blank');
    });

    it('should parse eval command', () => {
        const cmd = commands['eval'];
        const {toolName, toolParams} = parseCommand(cmd, args(['eval', 'document.title']));
        expect(toolName).toBe('browser_evaluate');
        expect(toolParams.expression).toBe('document.title');
    });

    it('should parse eval command with element ref', () => {
        const cmd = commands['eval'];
        const {toolName, toolParams} = parseCommand(cmd, args(['eval', 'el => el.textContent', 'e5']));
        expect(toolName).toBe('browser_evaluate');
        expect(toolParams.expression).toBe('el => el.textContent');
        expect(toolParams.ref).toBe('e5');
    });

    it('should parse dialog-accept command', () => {
        const cmd = commands['dialog-accept'];
        const {toolName, toolParams} = parseCommand(cmd, args(['dialog-accept']));
        expect(toolName).toBe('browser_handle_dialog');
        expect(toolParams.accept).toBe(true);
    });

    it('should parse dialog-accept with prompt text', () => {
        const cmd = commands['dialog-accept'];
        const {toolName, toolParams} = parseCommand(cmd, args(['dialog-accept', 'confirmation text']));
        expect(toolName).toBe('browser_handle_dialog');
        expect(toolParams.accept).toBe(true);
        expect(toolParams.promptText).toBe('confirmation text');
    });

    it('should parse dialog-dismiss command', () => {
        const cmd = commands['dialog-dismiss'];
        const {toolName, toolParams} = parseCommand(cmd, args(['dialog-dismiss']));
        expect(toolName).toBe('browser_handle_dialog');
        expect(toolParams.accept).toBe(false);
    });

    it('should parse go-back command', () => {
        const cmd = commands['go-back'];
        const {toolName} = parseCommand(cmd, args(['go-back']));
        expect(toolName).toBe('browser_navigate_back');
    });

    it('should parse go-forward command', () => {
        const cmd = commands['go-forward'];
        const {toolName} = parseCommand(cmd, args(['go-forward']));
        expect(toolName).toBe('browser_navigate_forward');
    });

    it('should parse reload command', () => {
        const cmd = commands['reload'];
        const {toolName} = parseCommand(cmd, args(['reload']));
        expect(toolName).toBe('browser_reload');
    });

    it('should parse snapshot with filename option', () => {
        const cmd = commands['snapshot'];
        const {toolName, toolParams} = parseCommand(cmd, {
            _: ['snapshot'],
            filename: 'after-click.yaml',
        } as any);
        expect(toolName).toBe('browser_snapshot');
        expect(toolParams.filename).toBe('after-click.yaml');
    });

    it('should parse screenshot command', () => {
        const cmd = commands['screenshot'];
        const {toolName, toolParams} = parseCommand(cmd, args(['screenshot']));
        expect(toolName).toBe('browser_take_screenshot');
    });

    it('should parse screenshot with ref', () => {
        const cmd = commands['screenshot'];
        const {toolName, toolParams} = parseCommand(cmd, args(['screenshot', 'e5']));
        expect(toolName).toBe('browser_take_screenshot');
        expect(toolParams.ref).toBe('e5');
    });

    it('should parse upload command', () => {
        const cmd = commands['upload'];
        const {toolName, toolParams} = parseCommand(cmd, args(['upload', '#file-input', './document.pdf']));
        expect(toolName).toBe('browser_file_upload');
        expect(toolParams.ref).toBe('#file-input');
        expect(toolParams.paths).toEqual(['./document.pdf']);
    });

    it('should parse check command', () => {
        const cmd = commands['check'];
        const {toolName, toolParams} = parseCommand(cmd, args(['check', 'e12']));
        expect(toolName).toBe('browser_check');
        expect(toolParams.ref).toBe('e12');
    });

    it('should parse uncheck command', () => {
        const cmd = commands['uncheck'];
        const {toolName, toolParams} = parseCommand(cmd, args(['uncheck', 'e12']));
        expect(toolName).toBe('browser_uncheck');
        expect(toolParams.ref).toBe('e12');
    });

    it('should parse keydown command', () => {
        const cmd = commands['keydown'];
        const {toolName, toolParams} = parseCommand(cmd, args(['keydown', 'Shift']));
        expect(toolName).toBe('browser_keydown');
        expect(toolParams.key).toBe('Shift');
    });

    it('should parse keyup command', () => {
        const cmd = commands['keyup'];
        const {toolName, toolParams} = parseCommand(cmd, args(['keyup', 'Shift']));
        expect(toolName).toBe('browser_keyup');
        expect(toolParams.key).toBe('Shift');
    });

    it('should parse mousedown command', () => {
        const cmd = commands['mousedown'];
        const {toolName, toolParams} = parseCommand(cmd, args(['mousedown', 'right']));
        expect(toolName).toBe('browser_mouse_down');
        expect(toolParams.button).toBe('right');
    });

    it('should parse mouseup command', () => {
        const cmd = commands['mouseup'];
        const {toolName, toolParams} = parseCommand(cmd, args(['mouseup', 'right']));
        expect(toolName).toBe('browser_mouse_up');
        expect(toolParams.button).toBe('right');
    });

    it('should parse tab-list command', () => {
        const cmd = commands['tab-list'];
        const {toolName, toolParams} = parseCommand(cmd, args(['tab-list']));
        expect(toolName).toBe('browser_tabs');
        expect(toolParams.action).toBe('list');
    });

    it('should parse tab-close without index', () => {
        const cmd = commands['tab-close'];
        const {toolName, toolParams} = parseCommand(cmd, args(['tab-close']));
        expect(toolName).toBe('browser_tabs');
        expect(toolParams.action).toBe('close');
    });

    it('should parse tab-close with index', () => {
        const cmd = commands['tab-close'];
        const {toolName, toolParams} = parseCommand(cmd, args(['tab-close', '2']));
        expect(toolName).toBe('browser_tabs');
        expect(toolParams.action).toBe('close');
        expect(toolParams.index).toBe(2);
    });

    it('should reject too many positional arguments', () => {
        const cmd = commands['goto'];
        expect(() => {
            parseCommand(cmd, args(['goto', 'https://example.com', 'extra']));
        }).toThrow('too many arguments');
    });
});
