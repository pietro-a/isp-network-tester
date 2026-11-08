#!/bin/bash

command -v git >/dev/null ||
{
    echo ""
    exit 1
}

git rev-parse --short HEAD 2>/dev/null ||
{
    echo ""
    exit 0
}
