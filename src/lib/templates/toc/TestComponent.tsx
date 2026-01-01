import { createElement } from '@/lib/utils/jsx';

export function TestComponent({ name = "World" }: { name?: string }) {
    return (
        <div style={{ 
            padding: '20px', 
            border: '2px dashed #007bff', 
            borderRadius: '8px',
            backgroundColor: '#e6f2ff',
            color: '#0056b3',
            fontWeight: 'bold',
            textAlign: 'center'
        }}>
            ✨ Hello from MDX Component, {name}! ✨
        </div>
    );
}
