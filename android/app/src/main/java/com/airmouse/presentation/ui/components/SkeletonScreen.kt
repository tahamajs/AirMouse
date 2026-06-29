package com.airmouse.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreenSkeleton(itemCount: Int = 5) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            ProfileSkeletonItem()
        }
    }
}

@Composable
fun ProfileSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerEffect(modifier = Modifier.size(50.dp, 50.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerEffect(modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp))
            ShimmerEffect(modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp))
        }
    }
}

@Composable
fun StatsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerEffect(modifier = Modifier.weight(1f).height(80.dp))
            ShimmerEffect(modifier = Modifier.weight(1f).height(80.dp))
        }
        ShimmerEffect(modifier = Modifier.fillMaxWidth().height(100.dp))
    }
}
