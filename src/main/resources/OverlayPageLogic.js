window.onload = () => {
    const webSocket = new WebSocket('ws://localhost:42020/socket');
    const overlay = document.querySelector('#overlay');
    const warning = document.querySelector('#warning');

    webSocket.onopen = () => {
        warning.style.visibility = 'hidden';
    };

    webSocket.onmessage = event => {
        const [x, y, width, height] = event.data.split(',').map(n => Number(n));

        Object.assign(overlay.style, {
            left: `${x}%`,
            top: `${y}%`,
            width: `${width}%`,
            height: `${height}%`,
        });
    };

    webSocket.onclose = webSocket.onerror = () => {
        warning.style.visibility = 'visible';
    };
};