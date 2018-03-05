class TextMeasurer {

    static _canvas = null;

    static _defaultLineHeight = 1.15;

    static getWidth = (text, fontFamily, fontSize) => {
        if (!TextMeasurer._canvas) {
            TextMeasurer._canvas = document.createElement("canvas");
        }
        const context = TextMeasurer._canvas.getContext("2d");
        context.font = `${fontSize}px ${fontFamily}`;
        return context.measureText(text).width;
    };

    static getHeight = (fontSize) =>
        TextMeasurer._defaultLineHeight * fontSize;

    static getSize = (text, fontFamily, fontSize) => ({
        width: TextMeasurer.getWidth(text, fontFamily, fontSize),
        height: TextMeasurer.getHeight(fontSize),
    });

}

export default TextMeasurer;
