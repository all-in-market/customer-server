resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpc"
  })
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-igw"
  })
}

resource "aws_subnet" "public" {
  for_each = {
    for idx, cidr in var.public_subnet_cidrs : idx => {
      cidr = cidr
      az   = var.availability_zones[idx]
    }
  }

  vpc_id                  = aws_vpc.this.id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.az
  map_public_ip_on_launch = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-subnet-public${tonumber(each.key) + 1}-${each.value.az}"
    Tier = "public"
  })
}

resource "aws_subnet" "ecs_private" {
  for_each = {
    for idx, cidr in var.ecs_private_subnet_cidrs : idx => {
      cidr = cidr
      az   = var.availability_zones[idx]
    }
  }

  vpc_id            = aws_vpc.this.id
  cidr_block        = each.value.cidr
  availability_zone = each.value.az

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-subnet-private${tonumber(each.key) + 1}-${each.value.az}"
    Tier = "private-app"
  })
}

resource "aws_subnet" "data_private" {
  for_each = {
    for idx, cidr in var.data_private_subnet_cidrs : idx => {
      cidr = cidr
      az   = var.availability_zones[idx]
    }
  }

  vpc_id            = aws_vpc.this.id
  cidr_block        = each.value.cidr
  availability_zone = each.value.az

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-subnet-private${tonumber(each.key) + 3}-${each.value.az}"
    Tier = "private-data"
  })
}

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-regional-nat-eip"
  })
}

resource "aws_nat_gateway" "this" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public["0"].id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-regional-nat"
  })

  depends_on = [aws_internet_gateway.this]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rtb-public"
  })
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

locals {
  private_subnet_route_table_map = {
    private1 = {
      subnet_id = aws_subnet.ecs_private["0"].id
      az        = var.availability_zones[0]
      number    = 1
    }
    private2 = {
      subnet_id = aws_subnet.ecs_private["1"].id
      az        = var.availability_zones[1]
      number    = 2
    }
    private3 = {
      subnet_id = aws_subnet.data_private["0"].id
      az        = var.availability_zones[0]
      number    = 3
    }
    private4 = {
      subnet_id = aws_subnet.data_private["1"].id
      az        = var.availability_zones[1]
      number    = 4
    }
  }
}

resource "aws_route_table" "private" {
  for_each = local.private_subnet_route_table_map

  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this.id
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rtb-private${each.value.number}-${each.value.az}"
  })
}

resource "aws_route_table_association" "private" {
  for_each = local.private_subnet_route_table_map

  subnet_id      = each.value.subnet_id
  route_table_id = aws_route_table.private[each.key].id
}