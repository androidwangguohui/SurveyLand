package com.example.surveyland.util

object StringUtils {

    open  fun getTdtStyleJson() : String{

        val key = "18200bf5ba2f674c772624185d27c1c9"

        return """
    {
      "version": 8,
      "sources": {
        "tdt-img": {
          "type": "raster",
          "tiles": [
            "https://t0.tianditu.gov.cn/img_w/wmts?service=wmts&request=GetTile&version=1.0.0&layer=img&style=default&tilematrixset=w&format=tiles&tilematrix={z}&tilerow={y}&tilecol={x}&tk=$key"
          ],
          "tileSize": 256
        },
        "tdt-cia": {
          "type": "raster",
          "tiles": [
            "https://t0.tianditu.gov.cn/cia_w/wmts?service=wmts&request=GetTile&version=1.0.0&layer=cia&style=default&tilematrixset=w&format=tiles&tilematrix={z}&tilerow={y}&tilecol={x}&tk=$key"
          ],
          "tileSize": 256
        }
      },
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": {
            "background-color": "#000000"
          }
        },
        {
          "id": "tdt-img",
          "type": "raster",
          "source": "tdt-img"
        },
        {
          "id": "tdt-cia",
          "type": "raster",
          "source": "tdt-cia"
        }
      ]
    }
    """.trimIndent()

    }
}