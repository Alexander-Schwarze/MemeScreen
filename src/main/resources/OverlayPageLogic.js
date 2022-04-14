window.onload = () => {
    const webSocket = new WebSocket('ws://localhost:42020/socket');
    const overlay = document.querySelector('#overlay');
    const warning = document.querySelector('#warning');

    webSocket.onopen = () => {
        warning.style.visibility = 'hidden';
    };

    webSocket.onmessage = event => {
        const [x, y, width, height, color] = event.data.split(',');

        Object.assign(overlay.style, {
            left: `${x}%`,
            top: `${y}%`,
            width: `${width}%`,
            height: `${height}%`,
            backgroundColor: color,
        });
    };

    webSocket.onclose = webSocket.onerror = () => {
        warning.style.visibility = 'visible';
    };
};