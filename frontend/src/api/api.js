const BASE_URL = 'http://localhost:8080/api'

export const sendVoiceCommand = async (command) => {
    const res = await fetch(`${BASE_URL}/voice-command`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command })
    })

    if (!res.ok) throw new Error('Voice command failed')

    return res.json()
}

export const getItems = async () => {
    const res = await fetch(`${BASE_URL}/items`)

    if (!res.ok) throw new Error('Failed to load items')

    return res.json()
}

export const clearItems = async () => {
    const res = await fetch(`${BASE_URL}/items`, {
        method: 'DELETE'
    })

    if (!res.ok) throw new Error('Failed to clear items')
}
