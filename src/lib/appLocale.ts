/** Select one of the supported UI locales from the system language. */
export function selectAppLocale(systemLanguage: string): 'en' | 'zh-CN' {
    const language = systemLanguage.toLowerCase().split(/[-_]/)[0];
    return language === 'zh' ? 'zh-CN' : 'en';
}
