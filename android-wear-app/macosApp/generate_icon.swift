import AppKit

func hexColor(_ hex: String) -> NSColor {
    var str = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    if str.hasPrefix("#") { str.removeFirst() }
    var rgb: UInt64 = 0
    Scanner(string: str).scanHexInt64(&rgb)
    let r = CGFloat((rgb >> 16) & 0xFF) / 255.0
    let g = CGFloat((rgb >> 8) & 0xFF) / 255.0
    let b = CGFloat(rgb & 0xFF) / 255.0
    return NSColor(srgbRed: r, green: g, blue: b, alpha: 1.0)
}

func renderIcon(pixelSize: Int) -> NSImage {
    let size = NSSize(width: pixelSize, height: pixelSize)
    let image = NSImage(size: size)
    image.lockFocus()

    guard let ctx = NSGraphicsContext.current?.cgContext else {
        image.unlockFocus()
        return image
    }

    let scale = CGFloat(pixelSize) / 108.0

    // macOS Big Sur squircle background
    let bgRect = NSRect(x: 0, y: 0, width: pixelSize, height: pixelSize)
    let cornerRadius = CGFloat(pixelSize) * 0.2237
    let squirclePath = NSBezierPath(roundedRect: bgRect, xRadius: cornerRadius, yRadius: cornerRadius)
    hexColor("0B1622").setFill()
    squirclePath.fill()
    squirclePath.addClip()

    // Inner subtle disc
    let circleRadius: CGFloat = 40.0 * scale
    let circleCenter = CGPoint(x: 54.0 * scale, y: CGFloat(pixelSize) - (54.0 * scale))
    let circlePath = NSBezierPath(ovalIn: NSRect(
        x: circleCenter.x - circleRadius,
        y: circleCenter.y - circleRadius,
        width: circleRadius * 2,
        height: circleRadius * 2
    ))
    hexColor("112236").setFill()
    circlePath.fill()

    // Flip Y-axis coordinate system to match Android vector drawable (0 at top)
    ctx.saveGState()
    ctx.translateBy(x: 0, y: CGFloat(pixelSize))
    ctx.scaleBy(x: 1.0, y: -1.0)

    func drawRect(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ color: String) {
        let rect = CGRect(x: x * scale, y: y * scale, width: w * scale, height: h * scale)
        let path = NSBezierPath(roundedRect: rect, xRadius: 1.5 * scale, yRadius: 1.5 * scale)
        hexColor(color).setFill()
        path.fill()
    }

    // 4 Market bar columns
    drawRect(31, 64, 8, 14, "1E4E79")
    drawRect(45, 54, 8, 24, "2563EB")
    drawRect(59, 46, 8, 32, "38BDF8")
    drawRect(73, 38, 6, 40, "0EA5E9")

    // Rising index trend line (#00E676)
    let linePath = NSBezierPath()
    linePath.move(to: CGPoint(x: 29.0 * scale, y: 58.0 * scale))
    linePath.line(to: CGPoint(x: 46.0 * scale, y: 45.0 * scale))
    linePath.line(to: CGPoint(x: 58.0 * scale, y: 51.0 * scale))
    linePath.line(to: CGPoint(x: 76.0 * scale, y: 30.0 * scale))
    linePath.lineWidth = 5.0 * scale
    linePath.lineCapStyle = .round
    linePath.lineJoinStyle = .round
    hexColor("00E676").setStroke()
    linePath.stroke()

    // Arrowhead at top-right peak (#00E676)
    let arrowPath = NSBezierPath()
    arrowPath.move(to: CGPoint(x: 67.0 * scale, y: 29.0 * scale))
    arrowPath.line(to: CGPoint(x: 79.0 * scale, y: 27.0 * scale))
    arrowPath.line(to: CGPoint(x: 77.0 * scale, y: 39.0 * scale))
    arrowPath.close()
    hexColor("00E676").setFill()
    arrowPath.fill()

    ctx.restoreGState()

    image.unlockFocus()
    return image
}

func savePNG(_ image: NSImage, to path: String) {
    guard let tiff = image.tiffRepresentation,
          let rep = NSBitmapImageRep(data: tiff),
          let png = rep.representation(using: .png, properties: [:]) else {
        return
    }
    try? png.write(to: URL(fileURLWithPath: path))
}

let fm = FileManager.default
let iconsetDir = "/tmp/MarketWear.iconset"
try? fm.removeItem(atPath: iconsetDir)
try? fm.createDirectory(atPath: iconsetDir, withIntermediateDirectories: true)

let specs: [(String, Int)] = [
    ("icon_16x16.png", 16),
    ("icon_16x16@2x.png", 32),
    ("icon_32x32.png", 32),
    ("icon_32x32@2x.png", 64),
    ("icon_128x128.png", 128),
    ("icon_128x128@2x.png", 256),
    ("icon_256x256.png", 256),
    ("icon_256x256@2x.png", 512),
    ("icon_512x512.png", 512),
    ("icon_512x512@2x.png", 1024)
]

for (filename, size) in specs {
    let img = renderIcon(pixelSize: size)
    savePNG(img, to: "\(iconsetDir)/\(filename)")
}

print("Iconset generated at \(iconsetDir)")
