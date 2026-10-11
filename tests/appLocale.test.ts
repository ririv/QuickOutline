import { strictEqual } from 'node:assert';
import { describe, test } from 'node:test';
import { selectAppLocale } from '../src/lib/appLocale';

describe('系统语言选择', () => {
    for (const language of ['en', 'en-US', 'en-GB', 'de-DE', 'ja-JP', 'C']) {
        test(`${language} 使用英文界面`, () => {
            strictEqual(selectAppLocale(language), 'en');
        });
    }

    for (const language of ['zh', 'zh-CN', 'zh-Hans-CN', 'zh-TW', 'zh_HK', 'ZH-cn']) {
        test(`${language} 使用中文界面`, () => {
            strictEqual(selectAppLocale(language), 'zh-CN');
        });
    }
});
